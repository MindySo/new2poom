import React, { useEffect, useRef, useState } from 'react';
import SideBar from '../../components/map/SideBar/SideBar';
import useKakaoMap from '../../hooks/useKakaoMap';
import Dashboard from '../../components/map/Dashboard/Dashboard';
import { useIsMobile } from '../../hooks/useMediaQuery';
import MyLocationButton from '../../components/map/MyLocationButton/MyLocationButton';
import MyLocationMarker from '../../components/map/MyLocationMarker/MyLocationMarker';
import MobileStatusBoard from '../../components/map/MobileStatusBoard/MobileStatusBoard';
import Marker from '../../components/map/Marker/Marker';
import type { MissingPerson } from '../../types/missing';
import styles from './MapPage.module.css';

const API_KEY = import.meta.env.VITE_KAKAO_JAVASCRIPT_KEY;

// 지도에 표시할 실종자 데이터 (위치 정보 포함)
export interface MissingPersonWithLocation extends MissingPerson {
  latitude: number;
  longitude: number;
}

// 더미 데이터
const DUMMY_MISSING_PERSONS: MissingPersonWithLocation[] = [
  {
    id: 1,
    personName: '김철수',
    ageAtTime: 8,
    currentAge: 10,
    gender: '남',
    nationality: '대한민국',
    occurredAt: '2023-03-15T14:30:00Z',
    occurredLocation: '서울특별시 종로구 청계천로',
    latitude: 37.5696,
    longitude: 126.9784,
    heightCm: 130,
    weightKg: 30,
    bodyType: '마른편',
    hairColor: '검정',
    hairStyle: '짧은 머리',
    clothingDesc: '파란색 후드티, 검정색 바지',
    progressStatus: '수색중',
    etcFeatures: '왼쪽 팔에 작은 점이 있음',
  },
  {
    id: 2,
    personName: '이영희',
    ageAtTime: 65,
    currentAge: 67,
    gender: '여',
    nationality: '대한민국',
    occurredAt: '2023-06-20T09:15:00Z',
    occurredLocation: '서울특별시 강남구 테헤란로',
    latitude: 37.5048,
    longitude: 127.0489,
    heightCm: 155,
    weightKg: 52,
    bodyType: '보통',
    hairColor: '흰색',
    hairStyle: '단발머리',
    clothingDesc: '베이지색 재킷, 회색 치마',
    progressStatus: '수색중',
    etcFeatures: '안경 착용',
  },
  {
    id: 3,
    personName: '박민수',
    ageAtTime: 15,
    currentAge: 16,
    gender: '남',
    nationality: '대한민국',
    occurredAt: '2023-09-05T18:45:00Z',
    occurredLocation: '서울특별시 마포구 홍대입구역',
    latitude: 37.5572,
    longitude: 126.9239,
    heightCm: 168,
    weightKg: 58,
    bodyType: '보통',
    hairColor: '검정',
    hairStyle: '중간 길이',
    clothingDesc: '회색 맨투맨, 청바지',
    progressStatus: '발견',
    etcFeatures: '오른쪽 귀에 피어싱',
  },
  {
    id: 4,
    personName: '최수진',
    ageAtTime: 32,
    currentAge: 33,
    gender: '여',
    nationality: '대한민국',
    occurredAt: '2023-11-12T21:00:00Z',
    occurredLocation: '서울특별시 송파구 잠실역',
    latitude: 37.5133,
    longitude: 127.1003,
    heightCm: 162,
    weightKg: 50,
    bodyType: '마른편',
    hairColor: '갈색',
    hairStyle: '긴 생머리',
    clothingDesc: '검정색 롱코트, 흰색 원피스',
    progressStatus: '수색중',
    etcFeatures: '목에 십자가 목걸이',
  },
  {
    id: 5,
    personName: '정대호',
    ageAtTime: 45,
    currentAge: 46,
    gender: '남',
    nationality: '대한민국',
    occurredAt: '2024-01-08T07:30:00Z',
    occurredLocation: '서울특별시 영등포구 여의도',
    latitude: 37.5219,
    longitude: 126.9245,
    heightCm: 175,
    weightKg: 72,
    bodyType: '보통',
    hairColor: '검정',
    hairStyle: '짧은 머리',
    clothingDesc: '검정색 정장, 회색 넥타이',
    progressStatus: '해결',
    etcFeatures: '안경 착용, 왼손에 결혼반지',
  },
  {
    id: 6,
    personName: '강지은',
    ageAtTime: 12,
    currentAge: 13,
    gender: '여',
    nationality: '대한민국',
    occurredAt: '2024-02-14T16:20:00Z',
    occurredLocation: '서울특별시 서초구 강남역',
    latitude: 37.4979,
    longitude: 127.0276,
    heightCm: 150,
    weightKg: 42,
    bodyType: '마른편',
    hairColor: '검정',
    hairStyle: '긴 생머리',
    clothingDesc: '분홍색 패딩, 청바지',
    progressStatus: '수색중',
    etcFeatures: '노란색 백팩 소지',
  },
  {
    id: 7,
    personName: '한준호',
    ageAtTime: 28,
    currentAge: 28,
    gender: '남',
    nationality: '대한민국',
    occurredAt: '2024-03-01T13:50:00Z',
    occurredLocation: '서울특별시 용산구 이태원',
    latitude: 37.5345,
    longitude: 126.9944,
    heightCm: 180,
    weightKg: 75,
    bodyType: '근육질',
    hairColor: '검정',
    hairStyle: '짧은 스포츠머리',
    clothingDesc: '검정색 가죽자켓, 청바지',
    progressStatus: '수색중',
    etcFeatures: '오른쪽 팔에 타투',
  },
  {
    id: 8,
    personName: '윤서아',
    ageAtTime: 55,
    currentAge: 56,
    gender: '여',
    nationality: '대한민국',
    occurredAt: '2024-03-10T10:30:00Z',
    occurredLocation: '서울특별시 성북구 성신여대입구역',
    latitude: 37.5925,
    longitude: 127.0167,
    heightCm: 158,
    weightKg: 55,
    bodyType: '보통',
    hairColor: '검정',
    hairStyle: '단발 파마',
    clothingDesc: '초록색 점퍼, 검정색 바지',
    progressStatus: '발견',
    etcFeatures: '지팡이 사용',
  },
];

const MapPage: React.FC = () => {
  const isMobile = useIsMobile(1024);

  const isLoaded = useKakaoMap(API_KEY);
  const mapRef = useRef<HTMLDivElement | null>(null);
  const [map, setMap] = useState<kakao.maps.Map | null>(null);
  const [isDashboardOpen, setIsDashboardOpen] = useState(false);
  const [myLocation, setMyLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [isLoadingLocation, setIsLoadingLocation] = useState(false);

  useEffect(() => {
    if (!isLoaded || !mapRef.current) return;

    const center = new kakao.maps.LatLng(37.5665, 126.9780); // 서울 중심
    const mapOptions = {
      center,
      level: 5, // 확대 레벨
    };

    const mapInstance = new kakao.maps.Map(mapRef.current, mapOptions);
    setMap(mapInstance);
  }, [isLoaded]);

  const handleOpenDashboard = () => {
    setIsDashboardOpen(true);
  };

  const handleCloseDashboard = () => {
    setIsDashboardOpen(false);
  };

  const handleMyLocation = () => {
    if (!map) return;

    if (isLoadingLocation) return;

    setIsLoadingLocation(true);

    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          const location = { lat: latitude, lng: longitude };

          setMyLocation(location);

          // 지도 중심을 내 위치로 이동
          const moveLatLon = new kakao.maps.LatLng(latitude, longitude);
          map.panTo(moveLatLon);

          // 줌 레벨 조정 (더 가까이)
          map.setLevel(3);

          setIsLoadingLocation(false);
        },
        (error) => {
          console.error('위치 정보를 가져오는데 실패했습니다:', error);
          alert('위치 정보를 가져올 수 없습니다. 브라우저 설정에서 위치 접근 권한을 확인해주세요.');
          setIsLoadingLocation(false);
        },
        {
          enableHighAccuracy: true,
          timeout: 5000,
          maximumAge: 0,
        }
      );
    } else {
      alert('이 브라우저는 위치 서비스를 지원하지 않습니다.');
      setIsLoadingLocation(false);
    }
  };

  return (
    <>
      {!isMobile && <SideBar onMissingCardClick={handleOpenDashboard} />}
      <div className={styles.mapContainer}>
        {!isLoaded && <p className={styles.loadingText}>지도를 불러오는 중...</p>}
        <div ref={mapRef} className={styles.mapElement} />

        {/* 모바일 상태 보드 */}
        {isMobile && (
          <div className={styles.mobileStatusBoardWrapper}>
            <MobileStatusBoard />
          </div>
        )}

        {/* 실종자 마커 */}
        {map && (
          <Marker
            map={map}
            position={{ lat: DUMMY_MISSING_PERSONS[0].latitude, lng: DUMMY_MISSING_PERSONS[0].longitude }}
            size="medium"
            onClick={() => console.log('마커 클릭:', DUMMY_MISSING_PERSONS[0].personName)}
          />
        )}

        {/* 내 위치 마커 */}
        {map && myLocation && (
          <MyLocationMarker
            map={map}
            position={myLocation}
          />
        )}

        {/* 내 위치 버튼 */}
        {map && (
          <MyLocationButton
            onClick={handleMyLocation}
            disabled={isLoadingLocation}
          />
        )}
      </div>

      <Dashboard isOpen={isDashboardOpen} onClose={handleCloseDashboard} />
    </>
  );
};

export default MapPage;
