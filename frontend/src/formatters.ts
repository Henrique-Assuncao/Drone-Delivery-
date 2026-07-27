export const measurementUnits = {
  weight: "kg",
  distance: "km",
  speed: "km/h"
} as const;

export function formatDuration(value: number) {
  if (!Number.isFinite(value) || value <= 0) {
    return "-";
  }

  if (value >= 60) {
    const totalMinutes = Math.round(value);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;

    return `${hours.toString().padStart(2, "0")}:${minutes
      .toString()
      .padStart(2, "0")} h`;
  }

  return `${value.toFixed(1)} min`;
}

export function formatNumber(value: number) {
  return value.toLocaleString("pt-BR", {
    maximumFractionDigits: 1
  });
}

export function formatWeight(value: number) {
  return `${formatNumber(value)} ${measurementUnits.weight}`;
}

export function formatDistance(value: number) {
  const absoluteValue = Math.abs(value);
  const maximumFractionDigits = absoluteValue > 0 && absoluteValue < 0.1 ? 3 : absoluteValue < 1 ? 2 : 1;

  return `${value.toLocaleString("pt-BR", { maximumFractionDigits })} ${measurementUnits.distance}`;
}

export function formatSpeed(value: number) {
  return `${formatNumber(value)} ${measurementUnits.speed}`;
}

export function formatLocation(x: number, y: number) {
  return `${formatDistance(x)}, ${formatDistance(y)}`;
}

export function minutesForDistance(distanceKilometers: number, speedKilometersPerHour: number) {
  return speedKilometersPerHour > 0 ? (distanceKilometers / speedKilometersPerHour) * 60 : 0;
}
