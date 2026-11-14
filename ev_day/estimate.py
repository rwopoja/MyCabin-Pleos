"""Transparent experimental EV planning estimates; not validated for driving decisions."""
from dataclasses import dataclass
from math import isfinite

@dataclass(frozen=True)
class Trip:
    distance_km: float
    efficiency_km_kwh: float
    stop_hours: float = 0
    stationary_kw: float = 0

def estimate(soc_percent, capacity_kwh, trips, reserve_percent=20):
    vals=[soc_percent,capacity_kwh,reserve_percent]
    if any(not isfinite(x) for x in vals) or not 0<=soc_percent<=100 or capacity_kwh<=0 or not 0<=reserve_percent<=100:
        raise ValueError('Invalid battery inputs')
    energy=0.0
    for t in trips:
        if any(not isfinite(x) for x in (t.distance_km,t.efficiency_km_kwh,t.stop_hours,t.stationary_kw)) or t.distance_km<0 or t.efficiency_km_kwh<=0 or t.stop_hours<0 or t.stationary_kw<0:
            raise ValueError('Invalid trip inputs')
        energy+=t.distance_km/t.efficiency_km_kwh+t.stop_hours*t.stationary_kw
    raw=soc_percent-100*energy/capacity_kwh
    return {'energy_kwh':energy,'estimated_soc_percent':max(0.0,raw),'reserve_margin_percent':raw-reserve_percent,'depleted_before_end':raw<0,'uncertainty':'not calibrated; estimate is not a guarantee'}
