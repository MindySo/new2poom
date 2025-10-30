import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { theme } from '../../../../theme';
import styles from './TopBar.module.css';

export interface TopBarProps {
  className?: string;
}

type NavItem = {
  label: string;
  path: string;
};

const navItems: NavItem[] = [
  { label: '실종자 지도페이지', path: '/map' },
  { label: '실종자 목록페이지', path: '/list' },
  { label: '기타 페이지', path: '/report' },
];

const TopBar: React.FC<TopBarProps> = ({ className = '' }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const handleNavClick = (path: string) => {
    navigate(path);
  };

  return (
    <div
      className={`${styles.topBar} ${className}`}
      style={{
        backgroundColor: theme.colors.beige,
      }}
    >
      {/* 로고 영역 */}
      <div className={styles.logoContainer}>
        <div
          className={styles.logoBox}
          style={{
            backgroundColor: theme.colors.beige,
            boxShadow: `4px 4px 12px rgba(232, 154, 89, 0.3)`,
          }}
        >
          <span
            className={styles.logoText}
            style={{
              color: theme.colors.darkMain,
            }}
          >
            품
          </span>
        </div>
      </div>

      {/* 네비게이션 영역 */}
      <nav className={styles.navbar}>
        {navItems.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <button
              key={item.path}
              className={`${styles.navButton} ${isActive ? styles.active : ''}`}
              style={{
                color: isActive ? theme.colors.darkMain : theme.colors.gray,
                fontWeight: isActive ? 600 : 500,
              }}
              onClick={() => handleNavClick(item.path)}
            >
              {isActive && (
                <span
                  className={styles.activeIndicator}
                  style={{
                    backgroundColor: theme.colors.main,
                  }}
                ></span>
              )}
              <span className={styles.navLabel}>{item.label}</span>
            </button>
          );
        })}
      </nav>
    </div>
  );
};

export default TopBar;
