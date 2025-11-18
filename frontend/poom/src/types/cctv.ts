export interface CctvDetection {
  id: number;
  similarityScore: number;
  latitude: number;
  longitude: number;
  cctvImageUrl: string;
  fullImageUrl: string;
  detectedAt: string; // ISO string
}

