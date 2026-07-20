import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useCarlogStore } from '@/features/carlog/carlogStore'
import { useCameraDataStore } from '@/features/camera-data/cameraDataStore'
import { useParkingsStore } from '@/features/parking/parkingsStore'

// 관리자 통계 페이지에서 사용할 store
// carlog, camera-data, parking 데이터를 가져와서 통계용 값으로 계산한다.
export const useStatisticsStore = defineStore('statistics', () => {
    const carlogStore = useCarlogStore()
    const cameraDataStore = useCameraDataStore()
    const parkingStore = useParkingsStore()

    // 통계 데이터를 불러오는 중인지 화면에서 표시할 때 사용한다.
    const loading = ref(false)

    // 데이터 조회 실패 시 화면에 보여줄 메시지다.
    const errorMessage = ref('')

    // 문자열 날짜를 Date 객체로 바꾸는 공통 함수
    // 잘못된 날짜가 들어오면 null을 반환해서 계산 오류를 막는다.
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

    // 특정 날짜가 기준 날짜와 같은 날인지 확인한다.
    // 입차 시간, 출차 시간이 오늘인지 확인할 때 사용한다.
    const isSameDate = (value, targetDate) => {
        const date = toDate(value)

        if (!date) {
            return false
        }

        return date.getFullYear() === targetDate.getFullYear()
            && date.getMonth() === targetDate.getMonth()
            && date.getDate() === targetDate.getDate()
    }

    // 그래프 아래에 표시할 날짜 형식
    // 예: 7월 20일이면 7/20으로 표시한다.
    const getDateLabel = (date) => {
        return `${date.getMonth() + 1}/${date.getDate()}`
    }

    // 오늘 날짜
    const today = computed(() => {
        return new Date()
    })

    // 최근 7일 날짜 배열
    // 막대그래프에서 7일치 데이터를 만들기 위해 사용한다.
    const recentSevenDays = computed(() => {
        return Array.from({ length: 7 }, (_, index) => {
            const date = new Date()
            date.setDate(date.getDate() - (6 - index))
            return date
        })
    })

    // 오늘 발생한 입출차 로그
    // inTime 또는 outTime이 오늘이면 오늘 로그로 본다.
    const todayCarlogs = computed(() => {
        return carlogStore.carLogs.filter((log) => {
            return isSameDate(log.inTime ?? log.outTime, today.value)
        })
    })

    // 오늘 입차 수
    // carlog.inTime이 오늘인 데이터만 센다.
    const todayInCount = computed(() => {
        return carlogStore.carLogs.filter((log) => {
            return isSameDate(log.inTime, today.value)
        }).length
    })

    // 오늘 출차 수
    // carlog.outTime이 오늘인 데이터만 센다.
    const todayOutCount = computed(() => {
        return carlogStore.carLogs.filter((log) => {
            return isSameDate(log.outTime, today.value)
        }).length
    })

    // 오늘 미등록 차량 수
    // carKind가 UNKNOWN이면 미등록 차량으로 본다.
    const todayUnknownCount = computed(() => {
        return todayCarlogs.value.filter((log) => {
            return log.carKind === 'UNKNOWN'
        }).length
    })

    // OCR 평균 신뢰도
    // 주의: 이 값은 정답률이 아니라 PaddleOCR이 반환한 confidenceScore 평균이다.
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

    // 최근 7일 입주민 차량 / 방문 차량 입차 비교 그래프용 데이터
    // REGISTERED는 입주민 차량, VISIT은 방문 차량으로 계산한다.
    const weeklyEntryStats = computed(() => {
        return recentSevenDays.value.map((date) => {
            const logs = carlogStore.carLogs.filter((log) => {
                return isSameDate(log.inTime, date)
            })

            const registeredCount = logs.filter((log) => {
                return log.carKind === 'REGISTERED'
            }).length

            const visitCount = logs.filter((log) => {
                return log.carKind === 'VISIT'
            }).length

            return {
                label: getDateLabel(date),
                registeredCount,
                visitCount,
            }
        })
    })

    // 최근 7일 입차 그래프의 최대값
    // 막대 높이를 계산할 때 기준값으로 사용한다.
    const weeklyMaxCount = computed(() => {
        const counts = weeklyEntryStats.value.flatMap((item) => {
            return [
                item.registeredCount,
                item.visitCount,
            ]
        })

        return Math.max(...counts, 1)
    })

    // 주차장별 사용률
    // parkingSpaces는 전체 자리 수, availableSpaces는 남은 자리 수다.
    // 사용 중인 자리 = 전체 자리 - 남은 자리
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

    // 오늘 입차 차량 유형 비율
    // 입주민 / 방문객 / 미등록 차량 비율을 계산한다.
    const todayEntryTypeStats = computed(() => {
        const total = todayInCount.value || 1

        const registered = todayCarlogs.value.filter((log) => {
            return log.carKind === 'REGISTERED'
                && isSameDate(log.inTime, today.value)
        }).length

        const visit = todayCarlogs.value.filter((log) => {
            return log.carKind === 'VISIT'
                && isSameDate(log.inTime, today.value)
        }).length

        const unknown = todayCarlogs.value.filter((log) => {
            return log.carKind === 'UNKNOWN'
                && isSameDate(log.inTime, today.value)
        }).length

        return [
            {
                label: '입주민',
                count: registered,
                percent: Math.round((registered / total) * 100),
            },
            {
                label: '방문객',
                count: visit,
                percent: Math.round((visit / total) * 100),
            },
            {
                label: '미등록',
                count: unknown,
                percent: Math.round((unknown / total) * 100),
            },
        ]
    })

    // 최근 7일 미등록 차량 추이
    // 관리자가 확인해야 하는 차량 흐름을 보여주기 위한 그래프 데이터다.
    const weeklyUnknownStats = computed(() => {
        return recentSevenDays.value.map((date) => {
            const count = carlogStore.carLogs.filter((log) => {
                return log.carKind === 'UNKNOWN'
                    && isSameDate(log.inTime ?? log.outTime, date)
            }).length

            return {
                label: getDateLabel(date),
                count,
            }
        })
    })

    // 미등록 차량 그래프의 최대값
    const weeklyUnknownMaxCount = computed(() => {
        const counts = weeklyUnknownStats.value.map((item) => {
            return item.count
        })

        return Math.max(...counts, 1)
    })

    // 통계 페이지에서 필요한 데이터를 한 번에 불러온다.
    // 기존 기능 store를 재사용해서 새 API 없이 빠르게 구성한다.
    const loadStatistics = async () => {
        loading.value = true
        errorMessage.value = ''

        try {
            await Promise.all([
                carlogStore.resetSearch(),
                cameraDataStore.loadList(),
                parkingStore.loadList(),
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

        todayInCount,
        todayOutCount,
        todayUnknownCount,
        averageConfidence,

        weeklyEntryStats,
        weeklyMaxCount,
        parkingUsageStats,
        todayEntryTypeStats,
        weeklyUnknownStats,
        weeklyUnknownMaxCount,

        loadStatistics,
    }
})