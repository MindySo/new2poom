import React from 'react';
import Text from '../../common/atoms/Text';
import StatusBoard from '../../map/StatusBoard/StatusBoard';
import RecentMissing from '../../map/RecentMissing/RecentMissing';
import { useRecentMissing } from '../../../hooks';
import styles from './PoliceSideBar.module.css';

export interface PoliceSideBarProps {
  className?: string;
  onMissingCardClick?: (id: number) => void;
}

// 실종 경과 시간 계산 함수
const getElapsedTime = (crawledAt: string): string => {
  const now = new Date();
  const crawled = new Date(crawledAt);
  const diffMs = now.getTime() - crawled.getTime();
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffHours / 24);

  if (diffDays > 0) {
    return `${diffDays}일 전`;
  } else if (diffHours > 0) {
    return `${diffHours}시간 전`;
  } else {
    return '1시간 이내';
  }
};

const PoliceSideBar: React.FC<PoliceSideBarProps> = ({ className = '', onMissingCardClick }) => {
  // 최근 72시간 내 실종자 데이터 가져오기
  const hours = 72;
  const { data: recentList, isLoading } = useRecentMissing(hours);
  
  // 경찰서 페이지용 색상
  const policeColor = '#2B3A55'; // darkMain 색상

  return (
    <aside
      className={`${styles.sideBar} ${className}`}
      style={{
        backgroundColor: policeColor,
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.2)',
      }}
    >
      {/* 현황판 */}
      <StatusBoard 
        textColor="white"
        borderColor="rgba(255, 255, 255, 0.3)"
      />

      {/* 최신 실종자 */}
      <div className={styles.recentMissingHeader}>
        <Text
          as="h2"
          size="xxl"
          weight="bold"
          color="white"
        >
          최근 실종자
        </Text>
      </div>

      {/* 최신 실종자 목록 */}
      <div className={styles.recentMissingList}>
        {isLoading ? (
          <div className={styles.emptyMessage}>
            <Text size="md" color="gray">로딩 중...</Text>
          </div>
        ) : recentList && recentList.length > 0 ? (
          recentList.map((person) => {
            // Badge 데이터 생성
            const badges: { text: string; variant?: 'time' | 'feature' | 'solved' | 'alert' | 'ai' }[] = [
              { text: getElapsedTime(person.crawledAt), variant: 'time' },
            ];

            // targetType이 있으면 추가
            if (person.targetType) {
              badges.push({ text: person.targetType, variant: 'feature' });
            }

            return (
              <RecentMissing
                key={person.id}
                image={person.mainImage?.url || 'https://via.placeholder.com/120'}
                badges={badges}
                name={person.personName}
                gender={person.gender || ''}
                age={person.ageAtTime}
                location={person.occurredLocation}
                onClick={() => onMissingCardClick?.(person.id)}
              />
            );
          })
        ) : (
          <div className={styles.emptyMessage}>
            <Text size="md" color="gray">최근 {hours}시간 내 실종자가 없습니다.</Text>
          </div>
        )}
      </div>
    </aside>
  );
};

export default PoliceSideBar;

