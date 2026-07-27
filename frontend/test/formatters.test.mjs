import assert from "node:assert/strict";
import test from "node:test";

import {
  formatDistance,
  formatDuration,
  formatLocation,
  formatNumber,
  formatSpeed,
  formatWeight,
  measurementUnits,
  minutesForDistance
} from "../dist-test/formatters.js";

test("formatDuration keeps invalid or empty values hidden", () => {
  assert.equal(formatDuration(0), "-");
  assert.equal(formatDuration(-5), "-");
  assert.equal(formatDuration(Number.NaN), "-");
});

test("formatDuration shows minutes while the value is below one hour", () => {
  assert.equal(formatDuration(12), "12.0 min");
  assert.equal(formatDuration(59.4), "59.4 min");
});

test("formatDuration converts values from sixty minutes to clock format", () => {
  assert.equal(formatDuration(60), "01:00 h");
  assert.equal(formatDuration(65), "01:05 h");
  assert.equal(formatDuration(125.4), "02:05 h");
});

test("formatters apply Brazilian metric units", () => {
  assert.deepEqual(measurementUnits, {
    weight: "kg",
    distance: "km",
    speed: "km/h"
  });
  assert.equal(formatNumber(12.34), "12,3");
  assert.equal(formatWeight(2.5), "2,5 kg");
  assert.equal(formatDistance(12.34), "12,3 km");
  assert.equal(formatDistance(0.075), "0,075 km");
  assert.equal(formatSpeed(40.25), "40,3 km/h");
  assert.equal(formatLocation(1.2, -0.075), "1,2 km, -0,075 km");
});

test("minutesForDistance converts kilometers and kilometers per hour to minutes", () => {
  assert.equal(minutesForDistance(30, 60), 30);
  assert.equal(minutesForDistance(7.5, 30), 15);
  assert.equal(minutesForDistance(10, 0), 0);
});
