import torch
import cv2
import numpy as np
from typing import Optional, Union
from PIL import Image
from transformers import pipeline


class InvSRFaceUpscaler:
    """OAOA/InvSR 모델을 사용한 간단한 얼굴 업스케일러"""
    
    def __init__(self, device: Optional[str] = None):
        self.device = device if device else ('cuda' if torch.cuda.is_available() else 'cpu')
        self.pipeline = None
        
    def load_model(self):
        """InvSR 모델 로드"""
        try:
            print("InvSR 모델 로드 중...")
            self.pipeline = pipeline(
                "image-to-image",
                model="OAOA/InvSR",
                device=0 if self.device == 'cuda' else -1
            )
            print("InvSR 모델 로드 완료")
            
        except Exception as e:
            print(f"모델 로드 실패: {e}")
            print("pip install transformers torch 필요")
            raise
            
    def upscale_face(self, image: Union[str, np.ndarray], 
                    output_path: Optional[str] = None) -> np.ndarray:
        """얼굴 업스케일링"""
        if self.pipeline is None:
            self.load_model()
            
        # 이미지 로드
        if isinstance(image, str):
            pil_image = Image.open(image).convert("RGB")
        else:
            rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            pil_image = Image.fromarray(rgb_image)
            
        try:
            # 업스케일링 수행
            result = self.pipeline(pil_image)
            
            # 결과 처리
            if hasattr(result, 'images'):
                result_pil = result.images[0]
            else:
                result_pil = result
                
            # PIL -> OpenCV 변환
            result_array = np.array(result_pil)
            result_bgr = cv2.cvtColor(result_array, cv2.COLOR_RGB2BGR)
            
            if output_path:
                cv2.imwrite(output_path, result_bgr)
                print(f"업스케일된 이미지 저장: {output_path}")
                
            return result_bgr
            
        except Exception as e:
            print(f"업스케일링 오류: {e}")
            raise


# 사용 예제
if __name__ == "__main__":
    upscaler = InvSRFaceUpscaler()
    
    # 이미지 파일 업스케일링
    result = upscaler.upscale_face("input_face.jpg", "output_upscaled.jpg")
    
    # 또는 numpy 배열 처리
    img = cv2.imread("face.jpg")
    upscaled = upscaler.upscale_face(img)
    cv2.imwrite("result.jpg", upscaled)