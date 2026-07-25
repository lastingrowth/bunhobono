import { reactive } from "vue";

// 현재 화면에 표시할 Dialog 정보입니다.
// App.vue에 배치할 AppDialog 컴포넌트가 이 상태를 읽습니다.
const dialogState = reactive({
    open: false,
    mode: "alert",
    theme: null,
    type: "info",
    title: "",
    message: "",
    caution: "",
    confirmText: "확인",
    cancelText: "취소"
});

// 사용자가 확인 또는 취소 버튼을 누를 때
// 호출한 페이지에 결과를 전달하기 위한 Promise 처리 함수입니다.
let dialogResolver = null;

// Dialog를 열고 사용자 선택을 기다립니다.
const openDialog = (options) => {
    return new Promise((resolve) => {
        dialogResolver = resolve;

        dialogState.mode = options.mode || "alert";

        // theme을 지정하지 않으면 AppDialog에서
        // 현재 주소에 따라 admin/resident 테마를 자동 선택합니다.
        dialogState.theme = options.theme || null;

        dialogState.type = options.type || "info";
        dialogState.title = options.title || "";
        dialogState.message = options.message || "";
        dialogState.caution = options.caution || "";

        dialogState.confirmText = options.confirmText || "확인";
        dialogState.cancelText = options.cancelText || "취소";
        dialogState.open = true;
    });
};

// alert 대체 함수입니다.
// 확인 버튼 하나만 표시합니다.
const alertDialog = (options = {}) => {
    return openDialog({
        ...options,
        mode: "alert"
    });
};

// confirm 대체 함수입니다.
// 취소와 확인 버튼을 함께 표시합니다.
const confirmDialog = (options = {}) => {
    return openDialog({
        ...options,
        mode: "confirm"
    });
};

// 확인 버튼을 눌렀을 때 실행합니다.
const confirmDialogAction = () => {
    dialogState.open = false;

    if (dialogResolver) {
        dialogResolver(true);
        dialogResolver = null;
    }
};

// 취소 버튼, 배경 또는 ESC를 눌렀을 때 실행합니다.
const cancelDialogAction = () => {
    dialogState.open = false;

    if (dialogResolver) {
        dialogResolver(false);
        dialogResolver = null;
    }
};

// 모든 화면에서 동일한 Dialog 상태와 함수를 사용합니다.
export const useDialog = () => {
    return {
        dialogState,
        alertDialog,
        confirmDialog,
        confirmDialogAction,
        cancelDialogAction
    };
};