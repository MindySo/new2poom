import boto3
import uuid

s3 = boto3.client(
    "s3",
    aws_access_key_id="YOUR_ACCESS_KEY",
    aws_secret_access_key="YOUR_SECRET_KEY",
    region_name="ap-northeast-2"
)

BUCKET_NAME = "your-bucket-name"

def upload_image_to_s3(pil_img, prefix="detections/"):
    buffer = io.BytesIO()
    pil_img.save(buffer, format="JPEG")
    buffer.seek(0)

    file_name = f"{prefix}{uuid.uuid4().hex}.jpg"

    s3.upload_fileobj(
        buffer,
        BUCKET_NAME,
        file_name,
        ExtraArgs={'ContentType': 'image/jpeg'}
    )

    # 반환: 퍼블릭 URL
    return f"https://{BUCKET_NAME}.s3.ap-northeast-2.amazonaws.com/{file_name}"
