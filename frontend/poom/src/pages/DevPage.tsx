import React from 'react';
import { Link } from 'react-router-dom';
import Button from '../components/common/atoms/Button';
import Badge from '../components/common/atoms/Badge';
import Text from '../components/common/atoms/Text';
import TopBar from '../components/common/molecules/TopBar';
import { theme } from '../theme';

const DevPage: React.FC = () => {
  return (
    <div style={{ 
      padding: '40px', 
      textAlign: 'center',
      fontFamily: 'Arial, sans-serif',
      backgroundColor: '#f8f9fa',
      minHeight: '100vh'
    }}>
      <h1 style={{ color: '#333', marginBottom: '30px' }}>
        🏠 품(POOM) 개발 페이지
      </h1>
      <p style={{ fontSize: '18px', color: '#666', marginBottom: '40px' }}>
        개발 중인 페이지들을 확인해보세요
      </p>
      
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        gap: '20px',
        flexWrap: 'wrap'
      }}>
        <Link 
          to="/map" 
          style={{ 
            padding: '20px 30px', 
            backgroundColor: '#007bff', 
            color: 'white', 
            textDecoration: 'none', 
            borderRadius: '10px',
            fontSize: '16px',
            fontWeight: 'bold',
            boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
            transition: 'transform 0.2s',
            display: 'block',
            minWidth: '150px'
          }}
          onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-2px)'}
          onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}
        >
          🗺️ 지도 페이지
        </Link>
        
        <Link 
          to="/list" 
          style={{ 
            padding: '20px 30px', 
            backgroundColor: '#28a745', 
            color: 'white', 
            textDecoration: 'none', 
            borderRadius: '10px',
            fontSize: '16px',
            fontWeight: 'bold',
            boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
            transition: 'transform 0.2s',
            display: 'block',
            minWidth: '150px'
          }}
          onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-2px)'}
          onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}
        >
          📋 목록 페이지
        </Link>
        
        <Link 
          to="/report" 
          style={{ 
            padding: '20px 30px', 
            backgroundColor: '#dc3545', 
            color: 'white', 
            textDecoration: 'none', 
            borderRadius: '10px',
            fontSize: '16px',
            fontWeight: 'bold',
            boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
            transition: 'transform 0.2s',
            display: 'block',
            minWidth: '150px'
          }}
          onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-2px)'}
          onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}
        >
          📝 신고 페이지
        </Link>
      </div>
      
      <div style={{
        marginTop: '50px',
        padding: '20px',
        backgroundColor: 'white',
        borderRadius: '10px',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        maxWidth: '600px',
        margin: '50px auto 0'
      }}>
        <h3 style={{ color: '#333', marginBottom: '15px' }}>개발 현황</h3>
        <p style={{ color: '#666', lineHeight: '1.6' }}>
          현재 3개의 주요 페이지가 개발 중입니다.<br/>
          각 페이지를 클릭하여 개발 상황을 확인해보세요.
        </p>
      </div>

      {/* Molecules 컴포넌트 테스트 섹션 */}
      <div style={{ maxWidth: '1200px', margin: '60px auto 0' }}>
        <h2 style={{ color: '#333', marginBottom: '30px', textAlign: 'center' }}>
          🧩 Molecules 컴포넌트 테스트
        </h2>

        {/* TopBar 컴포넌트 */}
        <div style={{
          backgroundColor: 'white',
          padding: '30px',
          borderRadius: '10px',
          marginBottom: '30px',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
        }}>
          <h3 style={{ marginBottom: '20px', color: '#333' }}>TopBar</h3>
          <p style={{ marginBottom: '15px', color: '#666', fontSize: '14px' }}>
            현재 페이지 경로에 따라 네비게이션 버튼이 활성화됩니다.
          </p>
          <div style={{ border: '1px solid #ddd', borderRadius: '8px', overflow: 'hidden' }}>
            <TopBar />
          </div>
        </div>
      </div>

      {/* Atoms 컴포넌트 테스트 섹션 */}
      <div style={{ maxWidth: '1200px', margin: '60px auto 0' }}>
        <h2 style={{ color: '#333', marginBottom: '30px', textAlign: 'center' }}>
          🎨 Atoms 컴포넌트 테스트
        </h2>

        {/* Text 컴포넌트 */}
        <div style={{
          backgroundColor: 'white',
          padding: '30px',
          borderRadius: '10px',
          marginBottom: '30px',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
        }}>
          <h3 style={{ marginBottom: '20px', color: '#333' }}>Text</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
            <Text size="hero" weight="bold">Hero 크기 텍스트</Text>
            <Text size="display" weight="bold">Display 크기 텍스트</Text>
            <Text size="xxxl" weight="semiBold">XXXL 크기 텍스트</Text>
            <Text size="xxl">XXL 크기 텍스트</Text>
            <Text size="xl">XL 크기 텍스트</Text>
            <Text size="lg">LG 크기 텍스트</Text>
            <Text size="md">MD 크기 텍스트</Text>
            <Text size="sm">SM 크기 텍스트</Text>
            <Text size="xs">XS 크기 텍스트</Text>
            <div style={{ display: 'flex', gap: '15px', marginTop: '10px', flexWrap: 'wrap' }}>
              <Text color="main">Main</Text>
              <Text color="darkMain">DarkMain</Text>
              <Text color="blue">Blue</Text>
              <Text color="green">Green</Text>
              <Text color="pink">Pink</Text>
              <Text color="yellow">Yellow</Text>
            </div>
          </div>
        </div>

        {/* Button 컴포넌트 */}
        <div style={{
          backgroundColor: 'white',
          padding: '30px',
          borderRadius: '10px',
          marginBottom: '30px',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
        }}>
          <h3 style={{ marginBottom: '20px', color: '#333' }}>Button</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div>
              <p style={{ marginBottom: '10px', color: '#666', fontSize: '14px' }}>Primary</p>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                <Button variant="primary" size="small">Small</Button>
                <Button variant="primary" size="medium">Medium</Button>
                <Button variant="primary" size="large">Large</Button>
                <Button variant="primary" disabled>Disabled</Button>
              </div>
            </div>
            <div>
              <p style={{ marginBottom: '10px', color: '#666', fontSize: '14px' }}>Secondary</p>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                <Button variant="secondary" size="small">Small</Button>
                <Button variant="secondary" size="medium">Medium</Button>
                <Button variant="secondary" size="large">Large</Button>
              </div>
            </div>
            <div>
              <p style={{ marginBottom: '10px', color: '#666', fontSize: '14px' }}>Dark Primary</p>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                <Button variant="darkPrimary" size="small">Small</Button>
                <Button variant="darkPrimary" size="medium">Medium</Button>
                <Button variant="darkPrimary" size="large">Large</Button>
              </div>
            </div>
            <div>
              <p style={{ marginBottom: '10px', color: '#666', fontSize: '14px' }}>Dark Secondary</p>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                <Button variant="darkSecondary" size="small">Small</Button>
                <Button variant="darkSecondary" size="medium">Medium</Button>
                <Button variant="darkSecondary" size="large">Large</Button>
              </div>
            </div>
          </div>
        </div>

        {/* Badge 컴포넌트 */}
        <div style={{
          backgroundColor: 'white',
          padding: '30px',
          borderRadius: '10px',
          marginBottom: '30px',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
        }}>
          <h3 style={{ marginBottom: '20px', color: '#333' }}>Badge (투명도 20%)</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div>
              <p style={{ marginBottom: '10px', color: '#666', fontSize: '14px' }}>Variants</p>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                <Badge variant="time">Time</Badge>
                <Badge variant="solved">Solved</Badge>
                <Badge variant="feature">Feature</Badge>
                <Badge variant="alert">Alert</Badge>
              </div>
            </div>
            <div>
              <p style={{ marginBottom: '10px', color: '#666', fontSize: '14px' }}>Sizes</p>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
                <Badge variant="time" size="small">Small</Badge>
                <Badge variant="solved" size="medium">Medium</Badge>
                <Badge variant="feature" size="large">Large</Badge>
              </div>
            </div>
          </div>
        </div>

        {/* Theme 색상 팔레트 */}
        <div style={{
          backgroundColor: 'white',
          padding: '30px',
          borderRadius: '10px',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
        }}>
          <h3 style={{ marginBottom: '20px', color: '#333' }}>Theme 색상 팔레트</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: '15px' }}>
            {Object.entries(theme.colors).map(([key, value]) => (
              <div key={key} style={{ textAlign: 'center' }}>
                <div style={{
                  width: '100%',
                  height: '80px',
                  background: value,
                  borderRadius: '8px',
                  marginBottom: '8px',
                  border: '1px solid #ddd'
                }}></div>
                <div style={{ fontSize: '12px', fontWeight: 'bold', color: '#333' }}>{key}</div>
                <div style={{ fontSize: '11px', color: '#999', wordBreak: 'break-all' }}>{value}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DevPage;
