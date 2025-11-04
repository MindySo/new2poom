import { useEffect, useState } from 'react';

// 전역 로딩 상태 관리
let isLoadingKakaoMap = false;
let kakaoMapLoadCallbacks: (() => void)[] = [];

export default function useKakaoMap(apiKey: string) {
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    console.log('[useKakaoMap] 시작, API Key:', apiKey);

    // API 키 검증
    if (!apiKey) {
      console.error('[useKakaoMap] API 키가 없습니다');
      return;
    }

    // 이미 로드된 경우
    if (window.kakao && window.kakao.maps) {
      console.log('[useKakaoMap] 이미 로드됨');
      setIsLoaded(true);
      return;
    }

    // 이미 로딩 중인 경우 콜백 등록
    if (isLoadingKakaoMap) {
      console.log('[useKakaoMap] 로딩 중, 콜백 등록');
      const callback = () => setIsLoaded(true);
      kakaoMapLoadCallbacks.push(callback);

      return () => {
        // cleanup: 콜백 제거
        kakaoMapLoadCallbacks = kakaoMapLoadCallbacks.filter(cb => cb !== callback);
      };
    }

    // 새로운 스크립트 로드 시작
    console.log('[useKakaoMap] 새 스크립트 추가 중...');
    isLoadingKakaoMap = true;

    const script = document.createElement('script');
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${apiKey}&autoload=false`;
    script.async = true;

    script.onload = () => {
      console.log('[useKakaoMap] 스크립트 로드 성공');
      window.kakao.maps.load(() => {
        console.log('[useKakaoMap] 카카오 맵 초기화 완료');
        isLoadingKakaoMap = false;
        setIsLoaded(true);

        // 대기 중인 모든 콜백 실행
        kakaoMapLoadCallbacks.forEach(callback => callback());
        kakaoMapLoadCallbacks = [];
      });
    };

    script.onerror = (error) => {
      console.error('[useKakaoMap] 스크립트 로드 실패:', error);
      console.error('[useKakaoMap] 스크립트 URL:', script.src);
      console.error('[useKakaoMap] ⚠️  카카오 개발자 콘솔에서 플랫폼 등록 확인 필요');
      console.error('[useKakaoMap] 👉 https://developers.kakao.com > 내 애플리케이션 > 앱 설정 > 플랫폼');
      console.error('[useKakaoMap] 👉 Web 플랫폼에 사이트 도메인 등록 (예: http://localhost:5173)');
      isLoadingKakaoMap = false;
      kakaoMapLoadCallbacks = [];
    };

    document.head.appendChild(script);

    // cleanup 시 스크립트 제거하지 않음 (전역적으로 한 번만 로드)
  }, [apiKey]);

  return isLoaded;
}
