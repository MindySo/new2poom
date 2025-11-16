import cv2
import torch
import numpy as np

device = "cuda" if torch.cuda.is_available() else "cpu"

def preprocess_image(img):
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = cv2.resize(img, (256, 128))
    img = img.astype(np.float32) / 255.0
    img = torch.tensor(img).permute(2, 0, 1).unsqueeze(0)
    return img.to(device)
