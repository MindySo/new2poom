import React, { useEffect, useRef, useState } from 'react';
import { useMissingDetail } from '../../../hooks';

/* 실제 스타일은 Kakao Maps의 Circle API에서 직접 처리됩니다 */

interface MovementRadiusProps {
  map: kakao.maps.Map;
  position: { lat: number; lng: number };
  radius: number; // 초기 반지름 (미터 단위)
  missingId?: number; // 실종자 ID (API 호출용)
}

const MovementRadius: React.FC<MovementRadiusProps> = ({ map, position, radius, missingId }) => {
  const circleRef = useRef<kakao.maps.Circle | null>(null);
  const [currentRadius, setCurrentRadius] = useState(radius);
  const [isMaxReached, setIsMaxReached] = useState(false);
  const animationRef = useRef<number | null>(null);
  const startTimeRef = useRef<number | null>(null);
  const [opacity, setOpacity] = useState(0); // 나타나기 애니메이션용 (0부터 시작)
  const fadeInStartedRef = useRef(false);
  const initialRadiusRef = useRef(radius); // 초기 반지름을 저장

  // useMissingDetail 훅으로 speed 데이터 가져오기
  const { data: missingPerson } = useMissingDetail(missingId || null);
  const speed = missingPerson?.speed || 0;

  // 나타나기 애니메이션 (Fade In) - 한 번만 실행
  useEffect(() => {
    if (fadeInStartedRef.current) return;

    fadeInStartedRef.current = true;

    let startTime: number | null = null;
    const FADE_IN_DURATION = 300; // 300ms

    const fadeIn = (currentTime: number) => {
      if (startTime === null) {
        startTime = currentTime;
      }

      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / FADE_IN_DURATION, 1);

      setOpacity(progress);

      if (progress < 1) {
        requestAnimationFrame(fadeIn);
      }
    };

    requestAnimationFrame(fadeIn);
  }, []);

  // 실시간 반지름 증가 애니메이션
  useEffect(() => {
    if (!map || speed === 0) return;

    const MAX_RADIUS = 60000; // 최대 반지름 60km (60000m) - 임시 테스트값

    const animate = (currentTime: number) => {
      if (startTimeRef.current === null) {
        startTimeRef.current = currentTime;
      }

      const elapsedSeconds = (currentTime - startTimeRef.current) / 1000;
      // speed는 km/h 단위이므로 m/s로 변환 (÷ 3.6)
        // (왜 3.6? → 1km = 1000m, 1시간 = 3600초 => 따라서 km/h를 m/s로: × 1000 ÷ 3600 = ÷ 3.6)
      const speedInMeterPerSecond = speed / 3.6;
      const newRadius = Math.min(initialRadiusRef.current + speedInMeterPerSecond * elapsedSeconds, MAX_RADIUS);

      setCurrentRadius(newRadius);

      // 최대값에 도달하지 않았으면 계속 애니메이션
      if (newRadius < MAX_RADIUS) {
        animationRef.current = requestAnimationFrame(animate);
      } else {
        // 최대값에 도달했으면 컴포넌트 사라지도록 설정
        setIsMaxReached(true);
        startTimeRef.current = null;
      }
    };

    animationRef.current = requestAnimationFrame(animate);

    // cleanup
    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [map, speed]);


  // 원(Circle) 생성 및 업데이트
  useEffect(() => {
    if (!map) return;

    // opacity가 0이거나 아주 작으면 원을 표시하지 않음
    if (opacity < 0.01) {
      if (circleRef.current) {
        circleRef.current.setMap(null);
        circleRef.current = null;
      }
      return;
    }

    // 기존 circle이 없으면 생성, 있으면 업데이트
    if (!circleRef.current) {
      const circle = new kakao.maps.Circle({
        center: new kakao.maps.LatLng(position.lat, position.lng),
        radius: currentRadius,
        strokeColor: '#0B72E7',
        strokeOpacity: opacity,
        strokeWeight: 2,
        fillColor: '#0B72E7',
        fillOpacity: 0.1 * opacity,
      });

      circle.setMap(map);
      circleRef.current = circle;
    } else {
      // 기존 circle 업데이트
      circleRef.current.setCenter(new kakao.maps.LatLng(position.lat, position.lng));
      circleRef.current.setRadius(currentRadius);
      circleRef.current.setOptions({
        strokeOpacity: opacity,
        fillOpacity: 0.1 * opacity,
      });
    }

    // cleanup
    return () => {
      if (circleRef.current) {
        circleRef.current.setMap(null);
        circleRef.current = null;
      }
    };
  }, [map, position.lat, position.lng, currentRadius, opacity]);

  return null;
};

export default MovementRadius;
