import React, { useEffect, useRef, useState } from 'react';
import PoliceSideBar from '../../components/police/PoliceSideBar/PoliceSideBar';
import useKakaoMap from '../../hooks/useKakaoMap';
import { useIsMobile, useRecentMissing } from '../../hooks';
import Marker from '../../components/map/Marker/Marker';
import styles from './PoliceMapPage.module.css';

const API_KEY = import.meta.env.VITE_KAKAO_JAVASCRIPT_KEY;

const PoliceMapPage: React.FC = () => {
  const isMobile = useIsMobile(1024);
  const isLoaded = useKakaoMap(API_KEY);
  const mapRef = useRef<HTMLDivElement | null>(null);
  const [map, setMap] = useState<kakao.maps.Map | null>(null);

  // 최근 72시간 내 실종자 데이터 가져오기 (마커용)
  const { data: recentMissingList } = useRecentMissing(72);

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

  const handleMissingCardClick = (id: number) => {
    // 경찰서 페이지에서 실종자 카드 클릭 시 처리
    if (map) {
      // TODO: 실종자 위치로 지도 이동 등의 기능 추가
      console.log('Missing person clicked:', id);
    }
  };

  return (
    <>
      {!isMobile && <PoliceSideBar onMissingCardClick={handleMissingCardClick} />}
      <div className={styles.mapContainer}>
        {!isLoaded && <p className={styles.loadingText}>지도를 불러오는 중...</p>}
        <div ref={mapRef} className={styles.mapElement} />

        {/* 실종자 마커 */}
        {map && recentMissingList && recentMissingList.map((person) => {
          // latitude와 longitude가 있는 경우만 마커 렌더링
          if (person.latitude && person.longitude) {
            return (
              <Marker
                key={person.id}
                map={map}
                position={{ lat: person.latitude, lng: person.longitude }}
                imageUrl={person.mainImage?.url}
                size="medium"
                onClick={() => handleMissingCardClick(person.id)}
              />
            );
          }
          return null;
        })}
      </div>
    </>
  );
};

export default PoliceMapPage;

