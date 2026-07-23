import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useCarlogStore } from '@/features/carlog/carlogStore'
import { useCameraDataStore } from '@/features/camera-data/cameraDataStore'
import { useParkingsStore } from '@/features/parking/parkingsStore'
import { useNoticeStore } from '@/features/notice/noticeStore'
import { useVehicleStore } from '@/features/vehicle/vehicleStore'
import { getTrashList } from '@/features/trash/trashApi'
import { parseDataJson } from '@/features/trash/trashFormat'

// 관리자 통계 페이지에서 사용할 store
// carlog, camera-data, parking, notice, vehicle 데이터를 가져와 화면용 통계 값으로 계산한다.
export const useStatisticsStore = defineStore('statistics', () => {
    const carlogStore = useCarlogStore()
    const cameraDataStore = useCameraDataStore()
    const parkingStore = useParkingsStore()
    const noticeStore = useNoticeStore()
    const vehicleStore = useVehicleStore()

    // 통계 데이터를 불러오는 중인지 표시한다.
    const loading = ref(false)

    // 통계 데이터 조회 실패 시 화면에 보여줄 메시지
    const errorMessage = ref('')

    // 지난 기록 통계 포함: 일반 목록에는 섞지 않고 통계 화면 내부에서만 사용한다.
    const archivedCarlogs = ref([])

    // [ARCHIVED STATISTICS]
    // Prefer the car_kind snapshot saved in trash_bin. Until the minimal backend
    // change is approved, fall back to the current vehicle list when possible.
    const resolveArchivedCarKind = (data) => {
        if (data.car_kind) {
            return data.car_kind
        }

        if (!data.vehicle_car_no) {
            return 'UNKNOWN'
        }

        const vehicle = vehicleStore.vehicleList.find((item) => {
            return Number(item.vehicleCarNo) === Number(data.vehicle_car_no)
        })

        if (!vehicle) {
            return 'UNKNOWN'
        }

        return vehicle.vehicleType === 'visit' ? 'VISIT' : 'REGISTERED'
    }

    // 지난 기록 통계 포함: trash_bin의 JSON을 기존 carlog 화면 형식으로 정규화한다.
    const normalizeArchivedCarlog = (item) => {
        const data = parseDataJson(item.dataJson ?? item.data_json)

        return {
            carLogNo: data.car_log_no,
            vehicleCarNo: data.vehicle_car_no,
            carNo: data.snapshot_car_no ?? data.captured_car_no,
            carKind: resolveArchivedCarKind(data),

            // [지난 기록 통계] 새 스냅샷을 우선 사용하고 기존 더미 car_kind도 호환한다.
            carKind: data.snapshot_car_kind
                ?? data.car_kind
                ?? resolveArchivedCarKind(data),

            inTime: data.in_time,
            outTime: data.out_time,
            statisticsScope: data.statistics_scope ?? 'ENTRY_AVERAGE',
            statisticsDate: data.statistics_date ?? data.in_time?.slice(0, 10),
            archived: true,
        }
    }

    // 지난 기록 통계 포함: 입차 비교와 평균시간에는 ENTRY_AVERAGE 자료만 합친다.
    const entryStatisticsLogs = computed(() => [
        ...carlogStore.carLogs,
        ...archivedCarlogs.value.filter((log) => {
            return log.statisticsScope === 'ENTRY_AVERAGE'
        }),
    ])

    // [ARCHIVED STATISTICS]
    // Scheduler archives keep normal in/out timestamps. Exclude demo HOURLY
    // snapshots here so real logs and snapshots are never counted together.
    const operationalStatisticsLogs = computed(() => [
        ...carlogStore.carLogs,
        ...archivedCarlogs.value.filter((log) => {
            return log.statisticsScope !== 'HOURLY'
        }),
    ])

    // 입주민 / 비입주민 입차 비교 그래프의 기간 상태
    // weekly: 주간, monthly: 월간, yearly: 연간
    const entryPeriod = ref('weekly')
    const entryPeriodOffset = ref(0)

    // 문자열 날짜를 Date 객체로 바꾼다.
    // 잘못된 날짜가 들어오면 계산 오류를 막기 위해 null을 반환한다.
    const toDate = (value) => {
        if (!value) {
            return null
        }

        const date = new Date(value)

        if (Number.isNaN(date.getTime())) {
            return null
        }

        return date
    }

    // 두 날짜가 같은 날짜인지 확인한다.
    const isSameDate = (value, targetDate) => {
        const date = toDate(value)

        if (!date) {
            return false
        }

        return date.getFullYear() === targetDate.getFullYear()
            && date.getMonth() === targetDate.getMonth()
            && date.getDate() === targetDate.getDate()
    }

    // 화면 그래프 아래에 표시할 날짜 라벨
    const getDateLabel = (date) => {
        return `${date.getMonth() + 1}/${date.getDate()}`
    }

    // 현재 날짜
    const today = ref(new Date())

    // 오늘 날짜 갱신
    const updateToday = () => {
        today.value = new Date()
    }

    // 최근 7일 날짜 배열
    const recentSevenDays = computed(() => {
        return Array.from({ length: 7 }, (_, index) => {
            const date = new Date()
            date.setDate(date.getDate() - (6 - index))
            return date
        })
    })

    // 현재 주차중인 입출차 로그
    // parkingState가 PARKING이거나, 출차 시간이 없는 로그를 현재 주차중으로 본다.
    const currentParkingLogs = computed(() => {
        return carlogStore.carLogs.filter((log) => {
            return log.parkingState === 'PARKING'
                || (log.inTime && !log.outTime)
        })
    })

    // 오늘 발생한 입출차 로그
    const todayCarlogs = computed(() => {
        return carlogStore.carLogs.filter((log) => {
            return isSameDate(log.inTime ?? log.outTime, today.value)
        })
    })

    // 오늘 입차 수
    const todayInCount = computed(() => {
        return carlogStore.carLogs.filter((log) => {
            return isSameDate(log.inTime, today.value)
        }).length
    })

    // 오늘 출차 수
    const todayOutCount = computed(() => {
        return carlogStore.carLogs.filter((log) => {
            return isSameDate(log.outTime, today.value)
        }).length
    })

    // 오늘 미등록 차량 수
    const todayUnknownCount = computed(() => {
        return todayCarlogs.value.filter((log) => {
            return log.carKind === 'UNKNOWN'
        }).length
    })

    // 평균 OCR 신뢰도
    // 주의: 정확도가 아니라 OCR 모델이 반환한 confidenceScore 평균이다.
    const averageConfidence = computed(() => {
        const scores = cameraDataStore.list
            .map((item) => Number(item.confidenceScore))
            .filter((score) => !Number.isNaN(score))

        if (scores.length === 0) {
            return 0
        }

        const total = scores.reduce((sum, score) => {
            return sum + score
        }, 0)

        return Math.round(total / scores.length)
    })

    // 현재 주차 현황 도넛 그래프
    // 현재 주차중인 차량을 입주민 / 방문차량 / 미등록 차량으로 나눈다.
    const currentParkingTypeStats = computed(() => {
        const residentCount = currentParkingLogs.value.filter((log) => {
            return log.carKind === 'REGISTERED'
        }).length

        const visitCount = currentParkingLogs.value.filter((log) => {
            return log.carKind === 'VISIT'
        }).length

        const unknownCount = currentParkingLogs.value.filter((log) => {
            return log.carKind === 'UNKNOWN'
        }).length

        const total = residentCount + visitCount + unknownCount || 1

        return [
            {
                key: 'resident',
                label: '입주민',
                count: residentCount,
                percent: Math.round((residentCount / total) * 100),
            },
            {
                key: 'visit',
                label: '방문',
                count: visitCount,
                percent: Math.round((visitCount / total) * 100),
            },
            {
                key: 'unknown',
                label: '미등록',
                count: unknownCount,
                percent: Math.round((unknownCount / total) * 100),
            },
        ]
    })

    // 현재 주차 현황 전체 수
    const currentParkingTotal = computed(() => {
        return currentParkingLogs.value.length
    })

    // 지난 기록 통계 포함: 최근 7개 완료일의 시간대별 평균을 계산한다.
    // HOURLY 자료를 분리해 입차 비교/평균시간과 중복 집계하지 않는다.
    // [ARCHIVED STATISTICS]
    // A demo date uses its HOURLY snapshots to preserve the requested graph shape.
    // A normal date without snapshots uses active and archived in/out intervals.
    const hourlyParkingStats = computed(() => {
        const hours = Array.from({ length: 9 }, (_, index) => index * 3)
        const hourlyLogs = archivedCarlogs.value.filter((log) => {
            return log.statisticsScope === 'HOURLY'
        })
        const completedDays = Array.from({ length: 7 }, (_, index) => {
            const date = new Date(today.value)
            date.setDate(date.getDate() - (index + 1))
            date.setHours(0, 0, 0, 0)
            return date
        })

        return hours.map((hour) => {
            const dailyCounts = completedDays.map((day) => {
                const target = new Date(day)
                target.setHours(hour, 0, 0, 0)
                const statisticsDate = [
                    day.getFullYear(),
                    String(day.getMonth() + 1).padStart(2, '0'),
                    String(day.getDate()).padStart(2, '0'),
                ].join('-')

                const snapshotLogs = hourlyLogs.filter((log) => {
                    return log.statisticsDate === statisticsDate
                })
                const sourceLogs = snapshotLogs.length > 0
                    ? snapshotLogs
                    : operationalStatisticsLogs.value

                return sourceLogs.filter((log) => {
                    const inTime = toDate(log.inTime)
                    const outTime = toDate(log.outTime)

                    return (snapshotLogs.length === 0
                        || log.statisticsDate === statisticsDate)
                        && inTime
                        && inTime <= target
                        && (!outTime || outTime >= target)
                }).length
            })

            const count = Math.round(
                dailyCounts.reduce((sum, value) => sum + value, 0)
                / completedDays.length
            )

            return {
                label: `${String(hour).padStart(2, '0')}시`,
                count,
            }
        })
    })

    // 시간대별 그래프 최대값
    const hourlyParkingMaxCount = computed(() => {
        const counts = hourlyParkingStats.value.map((item) => {
            return item.count
        })

        return Math.max(...counts, 1)
    })

    // 특정 기간 안의 입차 로그를 입주민 / 비입주민 수로 계산한다.
    // 비입주민은 방문차량과 미등록 차량을 합친 값이다.
    const getEntryCountByLogs = (logs) => {
        const residentCount = logs.filter((log) => {
            return log.carKind === 'REGISTERED'
        }).length

        const nonResidentCount = logs.filter((log) => {
            return log.carKind === 'VISIT' || log.carKind === 'UNKNOWN'
        }).length

        return {
            residentCount,
            nonResidentCount,
        }
    }

    // 주간 입차 비교 데이터
    // 최근 7일을 날짜별로 나눈다.
    const getWeeklyEntryStats = () => {
        const dates = recentSevenDays.value.map((date) => {
            const shiftedDate = new Date(date)
            shiftedDate.setDate(shiftedDate.getDate() + (entryPeriodOffset.value * 7))
            return shiftedDate
        })

        return dates.map((date) => {
            const logs = entryStatisticsLogs.value.filter((log) => {
                return isSameDate(log.inTime, date)
            })

            return {
                label: getDateLabel(date),
                ...getEntryCountByLogs(logs),
            }
        })
    }

    // 월간 입차 비교 데이터
    // 이번 달 데이터를 1주차 ~ 5주차로 나눈다.
    const getMonthlyEntryStats = () => {
        const targetMonth = new Date(today.value.getFullYear(), today.value.getMonth() + entryPeriodOffset.value, 1)
        const currentYear = targetMonth.getFullYear()
        const currentMonth = targetMonth.getMonth()

        return Array.from({ length: 5 }, (_, index) => {
            const weekNo = index + 1

            const logs = entryStatisticsLogs.value.filter((log) => {
                const inTime = toDate(log.inTime)

                if (!inTime) {
                    return false
                }

                const targetWeekNo = Math.ceil(inTime.getDate() / 7)

                return inTime.getFullYear() === currentYear
                    && inTime.getMonth() === currentMonth
                    && targetWeekNo === weekNo
            })

            return {
                label: `${weekNo}주차`,
                ...getEntryCountByLogs(logs),
            }
        })
    }

    // 연간 입차 비교 데이터
    // 올해 데이터를 1월 ~ 12월로 나눈다.
    const getYearlyEntryStats = () => {
        const currentYear = today.value.getFullYear() + entryPeriodOffset.value

        return Array.from({ length: 12 }, (_, index) => {
            const logs = entryStatisticsLogs.value.filter((log) => {
                const inTime = toDate(log.inTime)

                if (!inTime) {
                    return false
                }

                return inTime.getFullYear() === currentYear
                    && inTime.getMonth() === index
            })

            return {
                label: `${index + 1}월`,
                ...getEntryCountByLogs(logs),
            }
        })
    }

    // 입주민 / 비입주민 입차 비교 그래프 데이터
    // entryPeriod 값에 따라 주간 / 월간 / 연간 데이터로 바뀐다.
    const entryCompareStats = computed(() => {
        if (entryPeriod.value === 'monthly') {
            return getMonthlyEntryStats()
        }

        if (entryPeriod.value === 'yearly') {
            return getYearlyEntryStats()
        }

        return getWeeklyEntryStats()
    })

    // 입주민 / 비입주민 입차 비교 그래프 최대값
    const entryCompareMaxCount = computed(() => {
        const counts = entryCompareStats.value.flatMap((item) => {
            return [
                item.residentCount,
                item.nonResidentCount,
            ]
        })

        return Math.max(...counts, 1)
    })

    // 입차 비교 그래프 기간 변경
    const changeEntryPeriod = (period) => {
        entryPeriod.value = period
        entryPeriodOffset.value = 0
    }

    // 입차 비교 그래프의 이전/다음 조회 기간으로 이동한다.
    const moveEntryPeriod = (direction) => {
        if (direction === 'previous') {
            entryPeriodOffset.value -= 1
            return
        }

        entryPeriodOffset.value = Math.min(entryPeriodOffset.value + 1, 0)
    }

    const canMoveEntryPeriodNext = computed(() => entryPeriodOffset.value < 0)

    // 현재 그래프가 나타내는 날짜 범위를 상단에 표시한다.
    const entryPeriodLabel = computed(() => {
        if (entryPeriod.value === 'monthly') {
            const target = new Date(today.value.getFullYear(), today.value.getMonth() + entryPeriodOffset.value, 1)
            return `${target.getFullYear()}년 ${target.getMonth() + 1}월`
        }

        if (entryPeriod.value === 'yearly') {
            return `${today.value.getFullYear() + entryPeriodOffset.value}년`
        }

        const end = new Date(today.value)
        end.setDate(end.getDate() + (entryPeriodOffset.value * 7))
        const start = new Date(end)
        start.setDate(start.getDate() - 6)

        return `${start.getMonth() + 1}.${start.getDate()} ~ ${end.getMonth() + 1}.${end.getDate()}`
    })

    // 주차장별 현재 사용률
    const parkingUsageStats = computed(() => {
        return parkingStore.list.map((parking) => {
            const total = Number(parking.parkingSpaces ?? 0)
            const available = Number(parking.availableSpaces ?? total)
            const used = Math.max(total - available, 0)

            const percent = total > 0
                ? Math.round((used / total) * 100)
                : 0

            return {
                parkingNo: parking.parkingNo,
                parkingName: parking.parkingName,
                total,
                used,
                available,
                percent,
            }
        })
    })

    // 해결되지 않은 장기주차 알림
    // Resolved가 아닌 알림만 현재 주의 대상으로 본다.
    const activeNotices = computed(() => {
        return noticeStore.notices.filter((notice) => {
            const status = notice.alertStat ?? notice.alert_stat
            return status !== 'Resolved'
        })
    })

    // 비입주민 주차 주의 현황
    const nonResidentWarningStats = computed(() => {
        const visitLongStayCount = activeNotices.value.filter((notice) => {
            return notice.carKind === 'VISIT'
        }).length

        const unknownLongStayCount = activeNotices.value.filter((notice) => {
            return notice.carKind === 'UNKNOWN'
        }).length

        const parkedExpiredVehicleCount = vehicleStore.vehicleList.filter((vehicle) => {
            if (vehicle.vehicleType !== 'visit') {
                return false
            }

            const inTime = toDate(vehicle.inTime)
            const outTime = toDate(vehicle.outTime)
            const realEndDate = toDate(vehicle.realEndDate)

            if (!inTime || outTime || !realEndDate) {
                return false
            }

            const now = new Date()

            // 만료되었고, 아직 입차한 날짜와 같은 날
            return realEndDate <= now && isSameDate(inTime, now)
        }).length

        return [
            {
                key: 'visitLongStay',
                label: '방문 장기주차 알림',
                count: visitLongStayCount,
            },
            {
                key: 'unknownLongStay',
                label: '비등록 장기주차 알림',
                count: unknownLongStayCount,
            },
            {
                key: 'parkedExpired',
                label: '주차중 방문 만료 차량',
                count: parkedExpiredVehicleCount,
            },
        ]
    })

    // 분 단위를 화면 표시용 시간 문자열로 바꾼다.
    const formatMinutes = (minutes) => {
        const safeMinutes = Math.max(Math.round(minutes), 0)
        const hours = Math.floor(safeMinutes / 60)
        const remainMinutes = safeMinutes % 60

        if (hours > 0) {
            return `${hours}시간 ${remainMinutes}분`
        }

        return `${remainMinutes}분`
    }

    // 특정 차량 종류의 평균 주차 시간을 계산한다.
    const getAverageParkingMinutes = (carKind) => {
        const logs = entryStatisticsLogs.value.filter((log) => {
            return log.carKind === carKind && log.inTime
        })

        if (logs.length === 0) {
            return 0
        }

        const now = new Date()

        const totalMinutes = logs.reduce((sum, log) => {
            const inTime = toDate(log.inTime)
            const outTime = toDate(log.outTime) ?? now

            if (!inTime) {
                return sum
            }

            return sum + Math.max(Math.floor((outTime - inTime) / (1000 * 60)), 0)
        }, 0)

        return Math.round(totalMinutes / logs.length)
    }

    // 방문차량 / 미등록 차량 평균 주차 시간
    const averageParkingTimeStats = computed(() => {
        const visitMinutes = getAverageParkingMinutes('VISIT')
        const unknownMinutes = getAverageParkingMinutes('UNKNOWN')
        const maxMinutes = Math.max(visitMinutes, unknownMinutes, 1)

        return [
            {
                key: 'visit',
                label: '방문',
                minutes: visitMinutes,
                text: formatMinutes(visitMinutes),
                percent: Math.round((visitMinutes / maxMinutes) * 100),
            },
            {
                key: 'unknown',
                label: '미등록',
                minutes: unknownMinutes,
                text: formatMinutes(unknownMinutes),
                percent: Math.round((unknownMinutes / maxMinutes) * 100),
            },
        ]
    })

    // 통계 페이지에서 필요한 데이터를 한 번에 불러온다.
    const loadStatistics = async () => {
        loading.value = true
        errorMessage.value = ''

        try {
            // [지난 기록 통계 포함]
            // 백엔드 car_kind 스냅샷 적용 전 자료는 현재 차량 목록으로 임시 분류하므로
            // 차량 목록을 먼저 불러온 뒤 휴지통 JSON을 정규화한다.
            await vehicleStore.loadVehicleList()

            await Promise.all([
                carlogStore.resetSearch(),
                cameraDataStore.loadList(),
                parkingStore.loadList(),
                noticeStore.loadNotices(),
                // 지난 기록 통계 포함: CAR_LOG 휴지통 자료를 통계 화면에서만 조회한다.
                getTrashList('CAR_LOG').then((response) => {
                    archivedCarlogs.value = Array.isArray(response.data)
                        ? response.data.map(normalizeArchivedCarlog)
                        : []
                }),
            ])
        } catch (error) {
            console.error(error)
            errorMessage.value = '통계 데이터를 불러오지 못했습니다.'
        } finally {
            loading.value = false
        }
    }

    return {
        loading,
        errorMessage,

        entryPeriod,
        entryPeriodOffset,
        entryPeriodLabel,
        canMoveEntryPeriodNext,
        changeEntryPeriod,
        moveEntryPeriod,

        todayInCount,
        todayOutCount,
        todayUnknownCount,
        averageConfidence,

        currentParkingLogs,
        currentParkingTypeStats,
        currentParkingTotal,

        hourlyParkingStats,
        hourlyParkingMaxCount,

        entryCompareStats,
        entryCompareMaxCount,

        parkingUsageStats,
        nonResidentWarningStats,
        averageParkingTimeStats,

        loadStatistics,

        updateToday,
    }
})
