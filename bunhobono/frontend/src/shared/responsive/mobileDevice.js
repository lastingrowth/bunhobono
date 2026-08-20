// 터치를 지원하면서 화면의 짧은 쪽이 820px 이하인 기기를 모바일로 본다.
// screen의 짧은 쪽을 사용하므로 기기를 가로로 돌려도 판별 결과가 유지된다.
export function isMobileDevice() {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return false
  }

  const supportsTouch = navigator.maxTouchPoints > 0
  const shortSide = Math.min(window.screen.width, window.screen.height)

  return supportsTouch && shortSide <= 820
}

export function applyDeviceClass() {
  const mobile = isMobileDevice()

  document.documentElement.classList.toggle('mobile-device', mobile)
  document.documentElement.classList.toggle('desktop-device', !mobile)

  return mobile
}
