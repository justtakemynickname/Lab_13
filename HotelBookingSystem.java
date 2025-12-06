package lab_13;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HotelBookingSystem {
    private final State idle = new IdleState();
    private final State roomSelected = new RoomSelectedState();
    private final State bookingConfirmed = new BookingConfirmedState();
    private final State paid = new PaidState();
    private final State bookingCancelled = new BookingCancelledState();

    private State currentState = idle;
    private Booking currentBooking;
    private final List<Booking> history = new ArrayList<>();
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);

    interface State {
        void selectRoom(String room, double price);
        void changeRoom(String room, double price);
        void confirmBooking();
        void pay(double amount, double discountPercent);
        void cancel();
    }

    static class Booking {
        enum Status {NEW, CONFIRMED, PAID, COMPLETED, CANCELED}
        final int id;
        String room;
        double price;
        double paidAmount;
        double discountPercent;
        Status status;
        LocalDateTime createdAt;
        LocalDateTime completedAt;
        Booking(int id, String room, double price) {
            this.id = id;
            this.room = room;
            this.price = price;
            this.status = Status.NEW;
            this.createdAt = LocalDateTime.now();
        }
    }

    private class IdleState implements State {
        public void selectRoom(String room, double price) {
            currentBooking = new Booking(ID_GENERATOR.getAndIncrement(), room, price);
            currentState = roomSelected;
            System.out.println("Номер выбран: " + room + ", цена: " + price);
        }
        public void changeRoom(String room, double price) {
            System.out.println("Нечего менять");
        }
        public void confirmBooking() {
            System.out.println("Сначала выберите номер");
        }
        public void pay(double amount, double discountPercent) {
            System.out.println("Нельзя оплатить без выбора и подтверждения");
        }
        public void cancel() {
            System.out.println("Нет активного бронирования");
        }
    }

    private class RoomSelectedState implements State {
        public void selectRoom(String room, double price) {
            System.out.println("Уже выбран номер. Используйте changeRoom для изменения");
        }
        public void changeRoom(String room, double price) {
            currentBooking.room = room;
            currentBooking.price = price;
            System.out.println("Номер изменён на: " + room + ", новая цена: " + price);
        }
        public void confirmBooking() {
            currentBooking.status = Booking.Status.CONFIRMED;
            currentState = bookingConfirmed;
            System.out.println("Бронирование подтверждено: " + currentBooking.room);
        }
        public void pay(double amount, double discountPercent) {
            System.out.println("Сначала подтвердите бронирование");
        }
        public void cancel() {
            currentBooking.status = Booking.Status.CANCELED;
            currentState = bookingCancelled;
            currentState.cancel();
        }
    }

    private class BookingConfirmedState implements State {
        public void selectRoom(String room, double price) {
            System.out.println("Нельзя выбирать новый номер после подтверждения, отмените или измените до подтверждения");
        }
        public void changeRoom(String room, double price) {
            System.out.println("Нельзя изменить номер после подтверждения");
        }
        public void confirmBooking() {
            System.out.println("Уже подтверждено");
        }
        public void pay(double amount, double discountPercent) {
            double finalPrice = currentBooking.price * (1.0 - discountPercent / 100.0);
            if (amount < finalPrice) {
                System.out.println("Недостаточно средств. Требуется: " + finalPrice + ", внесено: " + amount);
                return;
            }
            currentBooking.paidAmount = amount;
            currentBooking.discountPercent = discountPercent;
            currentBooking.status = Booking.Status.PAID;
            currentState = paid;
            System.out.println("Оплата принята. Сумма: " + amount + ", скидка: " + discountPercent + "%");
            currentState = paid;
            ((PaidState) paid).finalizeBooking();
        }
        public void cancel() {
            currentBooking.status = Booking.Status.CANCELED;
            currentState = bookingCancelled;
            currentState.cancel();
        }
    }

    private class PaidState implements State {
        public void selectRoom(String room, double price) {
            System.out.println("Нельзя изменить после оплаты");
        }
        public void changeRoom(String room, double price) {
            System.out.println("Нельзя изменить после оплаты");
        }
        public void confirmBooking() {
            System.out.println("Уже оплачено");
        }
        public void pay(double amount, double discountPercent) {
            System.out.println("Уже оплачено");
        }
        public void cancel() {
            System.out.println("Нельзя отменить после оплаты");
        }
        void finalizeBooking() {
            currentBooking.status = Booking.Status.COMPLETED;
            currentBooking.completedAt = LocalDateTime.now();
            history.add(currentBooking);
            System.out.println("Бронирование завершено: ID=" + currentBooking.id + ", комната=" + currentBooking.room);
            currentBooking = null;
            currentState = idle;
        }
    }

    private class BookingCancelledState implements State {
        public void selectRoom(String room, double price) {
            System.out.println("Сначала завершите отмену");
        }
        public void changeRoom(String room, double price) {
            System.out.println("Сначала завершите отмену");
        }
        public void confirmBooking() {
            System.out.println("Отменено");
        }
        public void pay(double amount, double discountPercent) {
            System.out.println("Отменено");
        }
        public void cancel() {
            System.out.println("Бронирование отменено: ID=" + (currentBooking != null ? currentBooking.id : "-"));
            if (currentBooking != null) history.add(currentBooking);
            currentBooking = null;
            currentState = idle;
        }
    }

    public void selectRoom(String room, double price) {
        currentState.selectRoom(room, price);
    }
    public void changeRoom(String room, double price) {
        currentState.changeRoom(room, price);
    }
    public void confirmBooking() {
        currentState.confirmBooking();
    }
    public void pay(double amount, double discountPercent) {
        currentState.pay(amount, discountPercent);
    }
    public void cancel() {
        currentState.cancel();
    }
    public List<Booking> getHistory() {
        return new ArrayList<>(history);
    }

    public static void main(String[] args) {
        HotelBookingSystem s = new HotelBookingSystem();
        s.selectRoom("Single-101", 100.0);
        s.changeRoom("Single-102", 110.0);
        s.confirmBooking();
        s.pay(100.0, 10.0);

        s.selectRoom("Double-201", 200.0);
        s.confirmBooking();
        s.cancel();

        s.selectRoom("Suite-301", 500.0);
        s.confirmBooking();
        s.pay(500.0, 0.0);
        List<Booking> h = s.getHistory();
        System.out.println("История бронирований. Всего: " + h.size());
    }
}
