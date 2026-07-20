import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useCarlogStore } from '@/features/carlog/carlogStore'
import { useCameraDataStore } from '@/features/camera-data/cameraDataStore'
import { useParkingsStore } from '@/features/parking/parkingsStore'
import { useNoticeStore } from '@/features/notice/noticeStore'
import { useVehicleStore } from '@/features/vehicle/vehicleStore'

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

    // 입주민 / 비입주민 입차 비교 그래프의 기간 상태
    // weekly: 주간, monthly: 월간, yearly: 연간
    const entryPeriod = ref('weekly')

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
    const today = computed(() => {
        return new Date()
    })

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
                label: '방문차량',
                count: visitCount,
                percent: Math.round((visitCount / total) * 100),
            },
            {
                key: 'unknown',
                label: '미등록 차량',
                count: unknownCount,
                percent: Math.round((unknownCount / total) * 100),
            },
        ]
    })

    // 현재 주차 현황 전체 수
    const currentParkingTotal = computed(() => {
        return currentParkingLogs.value.length
    })

    // 시간대별 주차 대수
    // 현재 가진 carlog 데이터만으로 계산하므로, 실제 평균이라기보다 해당 시간대에 주차 상태였던 차량 수에 가깝다.
    const hourlyParkingStats = computed(() => {
        const hours = [0, 6, 12, 18, 24]

        return hours.map((hour) => {
            const target = new Date(today.value)

            if (hour === 24) {
                target.setDate(target.getDate() + 1)
                target.setHours(0, 0, 0, 0)
            } else {
                target.setHours(hour, 0, 0, 0)
            }

            const count = carlogStore.carLogs.filter((log) => {
                const inTime = toDate(log.inTime)
                const outTime = toDate(log.outTime)

                if (!inTime) {
                    return false
                }

                return inTime <= target && (!outTime || outTime >= target)
            }).length

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
        return recentSevenDays.value.map((date) => {
            const logs = carlogStore.carLogs.filter((log) => {
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
        const currentYear = today.value.getFullYear()
        const currentMonth = today.value.getMonth()

        return Array.from({ length: 5 }, (_, index) => {
            const weekNo = index + 1

            const logs = carlogStore.carLogs.filter((log) => {
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
        const currentYear = today.value.getFullYear()

        return Array.from({ length: 12 }, (_, index) => {
            const logs = carlogStore.carLogs.filter((log) => {
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
    }

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
            const parkedMinutes = Math.floor((now - inTime) / (1000 * 60))
            const oneDayMinutes = 60 * 24

            return realEndDate <= now && parkedMinutes < oneDayMinutes
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
        const logs = carlogStore.carLogs.filter((log) => {
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
                label: '방문차량',
                minutes: visitMinutes,
                text: formatMinutes(visitMinutes),
                percent: Math.round((visitMinutes / maxMinutes) * 100),
            },
            {
                key: 'unknown',
                label: '미등록 차량',
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
            await Promise.all([
                carlogStore.resetSearch(),
                cameraDataStore.loadList(),
                parkingStore.loadList(),
                noticeStore.loadNotices(),
                vehicleStore.loadVehicleList(),
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
        changeEntryPeriod,

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
    }
})