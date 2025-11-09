import React, { useRef, useEffect, useState } from 'react';
import styles from './MobileModal.module.css';

interface MobileModalProps {
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
  onOverlayClick?: () => void;
}

const MobileModal: React.FC<MobileModalProps> = ({ isOpen, onClose, children, onOverlayClick }) => {
  const modalRef = useRef<HTMLDivElement>(null);
  const handleRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [startY, setStartY] = useState(0);
  const [currentTranslate, setCurrentTranslate] = useState(0);
  const [expandedHeight, setExpandedHeight] = useState(0);
  const [isClosing, setIsClosing] = useState(false);

  // 모달 높이 상태 (0: 초기, 50: 절반, 100: 최대)
  const [modalState, setModalState] = useState<'initial' | 'half' | 'full'>('initial');

  // 손잡이 높이
  const HANDLE_HEIGHT = 48;
  // 초기 상태 높이: 손잡이만 표시 (약 48px)
  // 절반 상태 높이: 화면의 약 50%
  // 최대 상태 높이: 화면 높이 - MobileTopBar(약 104px) - 간격(?px)
  const INITIAL_HEIGHT = HANDLE_HEIGHT;
  const HALF_HEIGHT = window.innerHeight * 0.5;
  const FULL_HEIGHT = window.innerHeight - 200; // 전체 높이 - TopBar(104px) - 간격

  useEffect(() => {
    if (isOpen) {
      setIsClosing(false);
      setModalState('initial');
      setExpandedHeight(INITIAL_HEIGHT);
    } else if (!isOpen && expandedHeight > INITIAL_HEIGHT) {
      // 닫기 애니메이션 시작
      setIsClosing(true);
      const timer = setTimeout(() => {
        setIsClosing(false);
      }, 300);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  const handleMouseDown = (e: React.MouseEvent) => {
    if (!handleRef.current?.contains(e.target as Node)) return;

    setIsDragging(true);
    setStartY(e.clientY);
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    if (!handleRef.current?.contains(e.target as Node)) return;

    setIsDragging(true);
    setStartY(e.touches[0].clientY);
  };

  const handleMouseMove = (e: MouseEvent) => {
    if (!isDragging) return;

    const diff = startY - e.clientY;
    let newHeight = expandedHeight + diff;

    // 최소/최대 높이 제한
    newHeight = Math.max(INITIAL_HEIGHT, Math.min(FULL_HEIGHT, newHeight));
    setExpandedHeight(newHeight);
    setCurrentTranslate(diff);
  };

  const handleTouchMove = (e: TouchEvent) => {
    if (!isDragging) return;

    const diff = startY - e.touches[0].clientY;
    let newHeight = expandedHeight + diff;

    // 최소/최대 높이 제한
    newHeight = Math.max(INITIAL_HEIGHT, Math.min(FULL_HEIGHT, newHeight));
    setExpandedHeight(newHeight);
    setCurrentTranslate(diff);
  };

  const handleMouseUp = () => {
    if (!isDragging) return;
    setIsDragging(false);
    setCurrentTranslate(0);

    // 스냅 포인트로 이동
    if (expandedHeight < (INITIAL_HEIGHT + HALF_HEIGHT) / 2) {
      setExpandedHeight(INITIAL_HEIGHT);
      setModalState('initial');
    } else if (expandedHeight < (HALF_HEIGHT + FULL_HEIGHT) / 2) {
      setExpandedHeight(HALF_HEIGHT);
      setModalState('half');
    } else {
      setExpandedHeight(FULL_HEIGHT);
      setModalState('full');
    }
  };

  const handleTouchEnd = () => {
    if (!isDragging) return;
    setIsDragging(false);
    setCurrentTranslate(0);

    // 스냅 포인트로 이동
    if (expandedHeight < (INITIAL_HEIGHT + HALF_HEIGHT) / 2) {
      setExpandedHeight(INITIAL_HEIGHT);
      setModalState('initial');
    } else if (expandedHeight < (HALF_HEIGHT + FULL_HEIGHT) / 2) {
      setExpandedHeight(HALF_HEIGHT);
      setModalState('half');
    } else {
      setExpandedHeight(FULL_HEIGHT);
      setModalState('full');
    }
  };

  // 핸들 클릭 시 상태 전환
  const handleHandleClick = () => {
    if (modalState === 'initial') {
      setExpandedHeight(HALF_HEIGHT);
      setModalState('half');
    } else if (modalState === 'half') {
      setExpandedHeight(FULL_HEIGHT);
      setModalState('full');
    } else {
      setExpandedHeight(INITIAL_HEIGHT);
      setModalState('initial');
    }
  };

  // 모달 완전 닫기
  const closeModalCompletely = () => {
    setIsClosing(true);
    setTimeout(() => {
      onClose();
    }, 300);
  };

  // 배경 클릭 시 닫기
  const handleBackdropClick = () => {
    setExpandedHeight(INITIAL_HEIGHT);
    setModalState('initial');
    onClose();
  };

  // 손잡이만 보일 때 지도 클릭 시 완전히 닫기
  const handleOverlayClick = () => {
    if (modalState === 'initial') {
      closeModalCompletely();
      onOverlayClick?.();
    }
  };

  // 콘텐츠 스크롤 시 자동으로 모달 확장
  const handleContentScroll = () => {
    if (!contentRef.current) return;

    const scrollTop = contentRef.current.scrollTop;

    // 콘텐츠가 스크롤되면 모달을 최대 높이로 자동 확장
    if (scrollTop > 10 && modalState !== 'full') {
      setExpandedHeight(FULL_HEIGHT);
      setModalState('full');
    }
  };

  useEffect(() => {
    if (!isDragging) return;

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    window.addEventListener('touchmove', handleTouchMove);
    window.addEventListener('touchend', handleTouchEnd);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
      window.removeEventListener('touchmove', handleTouchMove);
      window.removeEventListener('touchend', handleTouchEnd);
    };
  }, [isDragging, expandedHeight, startY]);

  return (
    <>
      {/* 오버레이 (손잡이만 보일 때 지도 클릭 감지) */}
      {modalState === 'initial' && (
        <div
          className={styles.overlay}
          onClick={handleOverlayClick}
        />
      )}

      {/* 배경 */}
      <div
        className={`${styles.backdrop} ${isClosing ? styles.backdropClose : ''}`}
        onClick={handleBackdropClick}
        style={{
          opacity: modalState === 'full' ? 0.5 : 0,
          pointerEvents: modalState === 'full' ? 'auto' : 'none',
        }}
      />

      {/* 모달 */}
      <div
        ref={modalRef}
        className={`${styles.modalContainer} ${isClosing ? styles.modalClose : ''}`}
        style={{
          height: expandedHeight,
          transform: `translateY(${currentTranslate}px)`,
          transition: isDragging ? 'none' : 'height 0.3s ease, transform 0.3s ease',
        }}
      >
        {/* 손잡이 */}
        <div
          ref={handleRef}
          className={styles.handle}
          onClick={handleHandleClick}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              handleHandleClick();
            }
          }}
        >
          <div className={styles.handleBar} />
        </div>

        {/* 콘텐츠 */}
        <div
          ref={contentRef}
          className={styles.content}
          style={{
            maxHeight: expandedHeight - 48, // 손잡이 높이 제외
            overflowY: 'auto',
          }}
          onScroll={handleContentScroll}
        >
          {children}
        </div>
      </div>
    </>
  );
};

export default MobileModal;
