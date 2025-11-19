import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Text from '../../components/common/atoms/Text';
import Button from '../../components/common/atoms/Button';
import logoPolice from '../../assets/logo_police.png';
import styles from './PoliceLoginPage.module.css';

const PoliceLoginPage = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); // 에러 초기화

    // 임시 하드코딩된 계정 체크 (개발용)
    if (username === 'admin' && password === 'poom1234') {
      // 로그인 성공 시 localStorage에 저장
      localStorage.setItem('policeAuth', 'true');
      navigate('/police/map');
    } else {
      setError('아이디 또는 비밀번호가 일치하지 않습니다.');
    }

    // TODO: 로그인 API 호출 (나중에 백엔드 연동 시 사용)
    // console.log('Login attempt:', { username, password });
    // navigate('/police/map');
  };

  return (
    <div className={styles.container}>
      <div className={styles.logoContainer}>
        <img src={logoPolice} alt="품으로 로고" className={styles.logo} />
      </div>
      <div className={styles.loginCard}>
        <Text as="h1" size="xxl" weight="bold" color="black" className={styles.title}>
          품으로 실종자 관제 시스템
        </Text>
        
        <form onSubmit={handleLogin} className={styles.form}>
          <div className={styles.inputGroup}>
            <label className={styles.label}>
              <Text as="span" size="sm" weight="bold" color="black">
                아이디
              </Text>
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={styles.input}
              placeholder="아이디를 입력하세요"
              required
            />
          </div>

          <div className={styles.inputGroup}>
            <label className={styles.label}>
              <Text as="span" size="sm" weight="bold" color="black">
                비밀번호
              </Text>
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={styles.input}
              placeholder="비밀번호를 입력하세요"
              required
            />
          </div>

          {error && (
            <div className={styles.errorMessage}>
              <Text as="p" size="sm" weight="medium" color="black">
                {error}
              </Text>
            </div>
          )}

          <Button
            type="submit"
            variant="darkPrimary"
            size="large"
            fullWidth
            className={styles.loginButton}
          >
            로그인
          </Button>
        </form>
      </div>
    </div>
  );
};

export default PoliceLoginPage;

