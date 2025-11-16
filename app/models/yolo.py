from ultralytics import YOLO

yolo_model = YOLO("yolov8n.pt")

def detect_person(frame):
    results = yolo_model(frame)[0].boxes
    persons = []
    for box in results:
        if int(box.cls[0]) == 0:  # only person
            x1, y1, x2, y2 = map(int, box.xyxy[0])
            persons.append((x1, y1, x2, y2))
    return persons
