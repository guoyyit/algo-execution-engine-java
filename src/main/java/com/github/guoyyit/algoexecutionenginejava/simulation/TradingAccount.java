package com.github.guoyyit.algoexecutionenginejava.simulation;

import com.github.guoyyit.algoexecutionenginejava.model.Side;

import java.util.HashMap;
import java.util.Map;

public class TradingAccount {

    private final long maxPosition;
    private final long maxCash;

    private final Object lock = new Object();
    private final Map<String, Long> positions = new HashMap<>();
    private long usedCash;
    private long spentCash;
    private final Map<String, Reservation> reservations = new HashMap<>();

    public TradingAccount(long maxPosition, long maxCash) {
        this.maxPosition = maxPosition;
        this.maxCash = maxCash;
    }

    public boolean tryReserve(String parentOrderId, String symbol, Side side, long qty, long price) {
        synchronized (lock) {
            Reservation existing = reservations.get(parentOrderId);
            long baseReservedQty = existing == null ? 0L : existing.qty;
            long baseReservedCash = existing == null ? 0L : existing.cash;
            long currentPosition = positions.getOrDefault(symbol, 0L);
            long newReservedQty = baseReservedQty + qty;
            long newReservedCash = baseReservedCash + qty * price;
            long projectedPosition;
            long projectedCash;
            if (side == Side.BUY) {
                projectedPosition = currentPosition + newReservedQty;
                projectedCash = usedCash - baseReservedCash + newReservedCash;
            } else {
                projectedPosition = currentPosition - newReservedQty;
                projectedCash = usedCash;
            }
            if (projectedPosition > maxPosition || projectedPosition < -maxPosition || projectedCash > maxCash) {
                return false;
            }
            reservations.put(parentOrderId, new Reservation(symbol, side, newReservedQty, newReservedCash));
            if (side == Side.BUY) {
                usedCash = projectedCash;
            }
            return true;
        }
    }

    public void releaseReserved(String parentOrderId, String symbol, Side side, long qty, long price) {
        synchronized (lock) {
            Reservation reservation = reservations.get(parentOrderId);
            if (reservation == null) {
                return;
            }
            long releasedQty = Math.min(qty, reservation.qty);
            long releasedCash = releasedQty * price;
            reservation.qty -= releasedQty;
            reservation.cash = Math.max(0L, reservation.cash - releasedCash);
            if (reservation.side == Side.BUY) {
                usedCash = Math.max(0L, usedCash - releasedCash);
            }
            if (reservation.qty == 0L) {
                reservations.remove(parentOrderId);
            }
        }
    }

    public void onFill(String parentOrderId, String symbol, Side side, long qty, long price) {
        synchronized (lock) {
            long current = positions.getOrDefault(symbol, 0L);
            long next = side == Side.BUY ? current + qty : current - qty;
            positions.put(symbol, next);
            Reservation reservation = reservations.get(parentOrderId);
            if (reservation != null) {
                long filledQty = Math.min(qty, reservation.qty);
                long filledCash = filledQty * price;
                reservation.qty -= filledQty;
                reservation.cash = Math.max(0L, reservation.cash - filledCash);
                if (side == Side.BUY) {
                    spentCash += filledCash;
                }
                if (reservation.qty == 0L) {
                    reservations.remove(parentOrderId);
                }
            }
        }
    }

    public long position(String symbol) {
        synchronized (lock) {
            return positions.getOrDefault(symbol, 0L);
        }
    }

    public long usedCash() {
        synchronized (lock) {
            return spentCash + usedCash;
        }
    }

    private static final class Reservation {

        private final String symbol;
        private final Side side;
        private long qty;
        private long cash;

        private Reservation(String symbol, Side side, long qty, long cash) {
            this.symbol = symbol;
            this.side = side;
            this.qty = qty;
            this.cash = cash;
        }
    }
}
