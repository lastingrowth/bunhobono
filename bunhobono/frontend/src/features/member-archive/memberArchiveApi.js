import api from '@/shared/api/apiClient'

// 전출 확정되어 보관된 회원 이력 목록 조회
export const getMemberArchiveList = () => {
    return api.get('/member-archive')
}

// 전출 회원 이력 영구 삭제
// member 원본이 아니라 member_archive 기록만 삭제
export const deleteMemberArchive = (archiveNo) => {
    return api.delete(`/member-archive/${archiveNo}`)
}