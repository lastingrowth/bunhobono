import { useCarlogStore } from "@/features/carlog/carlogStore";
import { useGateStore } from "@/features/gates/gateStore";
import { useMemStore } from "@/features/member/memStore";
import { useNoticeStore } from "@/features/notice/noticeStore";
import { useParkingsStore } from "@/features/parking/parkingsStore";
import { useVehicleStore } from "@/features/vehicle/vehicleStore";
import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useAdminDashboardStore = defineStore('adminDashboard', () => {
    
    const noticeStore = useNoticeStore()
    const vehicleStore = useVehicleStore()
    const memberStore = useMemStore()

    const gateStore = useGateStore()
    const parkingStore = useParkingsStore()

    const carlogStore = useCarlogStore()

    const loading = ref(false)
    const errorMessage = ref('')

    // 관리자 대시보드 상단의 차량 등록 입력값
    // 현재 백엔드 정책상 등록 차량은 바로 승인되지 않고 WAITING 상태로 저장
    const quickVehicle = ref({
        carNo : '',
        memDong : '',
        memHo : '',
        memberNo : null,
    })

    // 주차장 카드별 현재 화면 상태
    const parkingCameraModes = ref({
        'A 주차장' : 'IN',
        'B 주차장' : 'IN',
        'C 주차장' : 'IN',
        'D 주차장' : 'IN'
    })

    // dialog 로 확대해서 보고 있는 주차장 카드
    const selectedParkingPanel = ref(null)

    // 오른쪽 입출차 로그에서 선택한 로그
    const selectedCarlog = ref(null)

    // 차량 번호 확인용 정규식
    // 예 : 12가3456, 서울12가 3456
    const carNoPattern = /^([가-힣]{2})?\d{2,3}[가-힣]\d{4}$/

    // 회원 목록에서 동 목록만 중복 제거해서 만든다
    const dongOptions = computed(() => {
        return [...new Set(
            memberStore.memberList
                .map((member) => member.memDong)
                .filter((dong) => dong !== null && dong !== undefined && dong !== '')
                .map((dong) => String(dong))
        )].sort((a,b) => Number(a) - Number(b))
    })

    // 선택한 동에 해당하는 호수 목록만 보여준다
    const hoOptions = computed(() => {
        return [...new Set(
            memberStore.memberList
                .filter((member) => {
                    return !quickVehicle.value.memDong 
                        || String(member.memDong ?? '') === quickVehicle.value.memDong
                })
                .map((member) => member.memHo)
                .filter((ho) => ho !== null && ho != undefined && ho !== '')
                .map((ho) => String(ho))        
        )].sort((a,b) => Number(a) - Number(b))
    })

    // 선택한 동/호수에 맞는 입주민만 보여준다
    const filteredMembers = computed(() => {
        return memberStore.memberList.filter((member) => {
            const dongMatched = !quickVehicle.value.memDong
                || String(member.memDong ?? '') === quickVehicle.value.memDong
            
            const hoMatched = !quickVehicle.value.memHo
                || String(member.memHo ?? '') === quickVehicle.value.memHo

            const isResident = member.role === 'RESIDENT' || member.role === 'resident'
            const isLiving = member.memStatus === '거주' || member.memStatus === 'APPROVED'

            return dongMatched && hoMatched && isResident && isLiving
        })
    })

    // 장기 주차 알림 수
    // 현재는 notice에서 처리되지 않은 알림을 장기 주차 알림으로 표시한다
    const longParkingNoticeCount = computed(() => {
        return noticeStore.notices.filter((notice) => {
            const status = notice.alertStat ?? notice.alert_stat

            return status === 'Unresolved'
        }).length
    })

    // 방문 차량 승인 대기 수
    const waitingVisitVehicleCount = computed(() => {
        return vehicleStore.vehicleList.filter((vehicle) => {
            return vehicle.vehicleType === 'visit'
                && vehicle.vehicleStatus === 'WAITING'
        }).length
    })

    // 회원가입 승인 대기 수
    const waitingMemberCount = computed(() => {
        return memberStore.memberList.filter((member) => {
            return member.role === 'PENDING'
        }).length
    })

    // 화면에서 v-for로 알림 칩을 출력하기 위한 배열
    const dashboardAlerts = computed(() => {
        return [
            {
                key : 'long-parking',
                title : '장기주차',
                count : longParkingNoticeCount.value,
                path : '/admin/notice',
            },
            {
                key : 'visit-approve',
                title : '방문승인',
                count : waitingVisitVehicleCount.value,
                path : '/admin/vehicles?mode=approve',
            },
                        {
                key : 'member-approve',
                title : '회원가입승인',
                count : waitingMemberCount.value,
                path : '/admin/members?section=pending',
            },
        ]
    })

    // 주차장 이름과 현재 화면 상태에 맞는 게이트를 찾는다
    const findGateByParking = (parkingName, gateType) => {
        return gateStore.list.find((gate) => {
            return gate.parkingName === parkingName
                && String(gate.gateType).toUpperCase() === gateType
        })
    }

    // A/B/C/D 주차장 모니터링 카드 목록
    // parkingStore.list에 있는 실제 주차장 정보를 기준으로 만든다
    const parkingMonitorPanels = computed(() => {
        return parkingStore.list.slice(0,4).map((parking) => {
            const mode = parkingCameraModes.value[parking.parkingName] ?? 'IN'
            const gate = findGateByParking(parking.parkingName, mode)

            return {
                parkingNo : parking.parkingNo,
                parkingName : parking.parkingName,
                parkingLocation : parking.parkingLocation,
                mode,
                modeText : mode === 'IN' ? '입차' : '출차',
                modeClass : mode === 'IN' ? 'in' : 'out',
                gate,
            }
        })
    })

    // dialog가 열려 있을 때, 갱신된 주차장/게이트 정보로 선택 패널을 다시 맞춘다.
    const refreshSelectedParkingPanel = () => {
        if (!selectedParkingPanel.value) {
            return
        }

        const latestPanel = parkingMonitorPanels.value.find((panel) => {
            return Number(panel.parkingNo) === Number(selectedParkingPanel.value.parkingNo)
        })

        selectedParkingPanel.value = latestPanel ?? selectedParkingPanel.value
    }

    // 대시 보드 오른쪽에 보여줄 최신 입출차 로그
    const recentCarlogs = computed(() => {
        return [...carlogStore.carLogs]
            .sort((left,right) => {
                const rightTime = new Date(right.inTime ?? right.outTime ?? 0)
                const leftTime = new Date(left.inTime ?? left.outTime ?? 0)

                return rightTime - leftTime
            })
            .slice(0,7)
    })

    // 입차/출차 버튼을 눌렀을 때 해당 주차장의 화면 상태를 바꾼다
    const toggleParkingCamera = (parkingName) => {
        const currentMode = parkingCameraModes.value[parkingName] ?? 'IN'

        parkingCameraModes.value = {
            ...parkingCameraModes.value,
            [parkingName] : currentMode === 'IN' ? 'OUT' : 'IN'
        }

        refreshSelectedParkingPanel()
    }

    // 주차장 카드를 클릭하면 dialog에 표시할 데이터를 저장한다
    const selectParkingPanel = (panel) => {
        selectedParkingPanel.value = panel
    }

    // dialog를 닫을 때 선택된 주차장 데이터를 비운다
    const closeParkingPanel = () => {
        selectedParkingPanel.value = null
    }

    // 입출차 로그 행을 클릭했을 때 상세 정보에 표시한다
    const selectCarlog = (log) => {
        selectedCarlog.value = log
    }

    // 주차 상태별 색상 class를 정한다.
    const parkingStateClass = (log) => {
        if (log.parkingState === 'PARKING') {
            return 'parking'
        }

        if (log.parkingState === 'OUT') {
            return 'out'
        }

        return 'unknown'
    }

    // 관리자가 수동으로 게이트를 여는 함수
    // 백엔드 open API가 5초 뒤 자동 닫힘까지 처리
    const openManualGate = async (gate) => {
        if (!gate) {
            alert('연결된 게이트가 없습니다')
            return
        }

        // 백엔드 /open API를 호출한다.
        // 백엔드에서 게이트를 열고, scheduleClose로 자동 닫힘까지 예약한다.
        await gateStore.open(gate.gateNo)

        // 열림 상태를 대시보드에 바로 반영한다.
        await gateStore.loadList()
        refreshSelectedParkingPanel()

        // 백엔드 자동 닫힘 시간이 지난 뒤 다시 조회해서 닫힘 상태를 화면에 반영한다.
        setTimeout(async () => {
            await gateStore.loadList()
            refreshSelectedParkingPanel()
        }, 5500)
    }

    // 동 표시 형식
    const dongText = (dong) => {
        if (String(dong) === '0') {
            return '관리동'
        }

        return `${dong}동`
    }

    // 호수 표시 형식
    const hoText = (ho) => {
        if (String(ho) === '0') {
            return '관리실'
        }

        return `${ho}호`
    }

    // 입주민 select에 표시할 문구
    const memberLabel = (member) => {
        return `${dongText(member.memDong)} ${hoText(member.memHo)} / ${member.memName}`
    }

    // 차량 등록 입력값 초기화
    const resetQuickVehicle = () => {
        quickVehicle.value = {
            carNo : '',
            memDong : '',
            memHo : '',
            memberNo : null,            
        }
    }

    // 관리자 대시보드에서 입주민 차량을 등록
    // 현재 백엔드 VehicleServie 에서 vehicleStatus를 WAITING으로 고정하므로 승인 대기 상태로 저장
    const submitQuickVehicle = async () => {
        const normalizedCarNo = quickVehicle.value.carNo.trim().replace(/\s/g, '')

        if (normalizedCarNo === '') {
            alert('차량번호를 입력하세요')
            return
        }

        if (!carNoPattern.test(normalizedCarNo)) {
            alert('차량번호 형식이 올바르지 않습니다. 예: 12가3456')
            return
        }

        if (!quickVehicle.value.memberNo) {
            alert('입주민을 선택하세요')
            return
        }

        try {
            await vehicleStore.addVehicle({
                carNo : normalizedCarNo,
                vehicleType : 'normal',
                memberNo : quickVehicle.value.memberNo,
            })

            alert('차량이 승인 대기 상태로 등록되었습니다')
            resetQuickVehicle()
        } catch (e) {
            console.error(e)

            if (e.response?.status === 409) {
                alert('이미 등록 또는 승인 대기 중인 차량번호입니다')
                return
            }

            alert('차량 등록 중 오류가 발생했습니다')
        }
    }

    // 대시보드 진입 시 상단 카드에 필요한 데이터를 조회한다
    const loadDashboard = async () => {
        loading.value = true
        errorMessage.value = ''

        try {
            await Promise.all([
                noticeStore.loadNotices(),
                vehicleStore.loadVehicleList(),
                memberStore.loadmemberList(),
                parkingStore.loadList(),
                gateStore.loadList(),
                carlogStore.loadCarLogs()
            ])

            selectedCarlog.value = recentCarlogs.value[0] ?? null
        } catch (e) {
            console.log(e)
            errorMessage.value = '대시보드 정보를 불러오지 못했습니다'
        } finally {
            loading.value = false
        }
    }

    return {
        loading,
        errorMessage,

        quickVehicle,
        dongOptions,
        hoOptions,
        filteredMembers,
        dashboardAlerts,
        parkingMonitorPanels,
        selectedParkingPanel,
        recentCarlogs,
        selectedCarlog,

        dongText,
        hoText,
        memberLabel,
        submitQuickVehicle,
        toggleParkingCamera,
        selectParkingPanel,
        closeParkingPanel,
        selectCarlog,
        parkingStateClass,
        openManualGate,
        loadDashboard,
    }
})