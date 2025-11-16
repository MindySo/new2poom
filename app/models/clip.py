import torch
import numpy as np
from PIL import Image
from transformers import CLIPProcessor, CLIPModel
from sklearn.preprocessing import normalize

device = "cuda" if torch.cuda.is_available() else "cpu"

clip_model = CLIPModel.from_pretrained("Bingsu/clip-vit-base-patch32-ko").to(device)
clip_processor = CLIPProcessor.from_pretrained("Bingsu/clip-vit-base-patch32-ko")

def extract_clip_image(image: Image.Image):
    with torch.no_grad():
        inputs = clip_processor(images=image, return_tensors="pt").to(device)
        feat = clip_model.get_image_features(**inputs)
        feat = normalize(feat.cpu().numpy(), axis=1)
    return feat

def extract_clip_text(text: str):
    with torch.no_grad():
        inputs = clip_processor(text=[text], return_tensors="pt").to(device)
        feat = clip_model.get_text_features(**inputs)
        feat = normalize(feat.cpu().numpy(), axis=1)
    return feat
