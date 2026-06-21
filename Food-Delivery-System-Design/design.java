import java.util.*;

public class FoodDeliveryApp {

    // =====================================================
    // ENUMS
    // =====================================================

    enum CuisineType {
        INDIAN, CHINESE, ITALIAN
    }

    enum MealType {
        BREAKFAST, LUNCH, DINNER
    }

    enum OrderStatus {
        CREATED,
        CONFIRMED,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED
    }

    // =====================================================
    // MODELS
    // =====================================================

    static class User {
        String id;
        String name;
        String city;
        Cart cart = new Cart();

        public User(String id, String name, String city) {
            this.id = id;
            this.name = name;
            this.city = city;
        }
    }

    static class Restaurant {
        String id;
        String name;
        String city;
        List<FoodItem> menu = new ArrayList<>();

        public Restaurant(String id, String name, String city) {
            this.id = id;
            this.name = name;
            this.city = city;
        }

        void addFoodItem(FoodItem item) {
            menu.add(item);
        }
    }

    static class FoodItem {
        String id;
        String name;
        double price;
        CuisineType cuisineType;
        MealType mealType;

        public FoodItem(String id,
                        String name,
                        double price,
                        CuisineType cuisineType,
                        MealType mealType) {

            this.id = id;
            this.name = name;
            this.price = price;
            this.cuisineType = cuisineType;
            this.mealType = mealType;
        }
    }

    static class CartItem {
        FoodItem foodItem;
        int quantity;

        public CartItem(FoodItem foodItem, int quantity) {
            this.foodItem = foodItem;
            this.quantity = quantity;
        }

        double getTotalPrice() {
            return quantity * foodItem.price;
        }
    }

    static class Cart {

        Map<String, CartItem> items = new HashMap<>();

        void addItem(FoodItem foodItem, int qty) {

            items.put(
                    foodItem.id,
                    new CartItem(foodItem, qty)
            );
        }

        void removeItem(String foodId) {
            items.remove(foodId);
        }

        Collection<CartItem> getItems() {
            return items.values();
        }

        void clear() {
            items.clear();
        }
    }

    static class Bill {
        double subtotal;
        double discount;
        double tax;
        double finalAmount;

        @Override
        public String toString() {
            return "Bill{" +
                    "subtotal=" + subtotal +
                    ", discount=" + discount +
                    ", tax=" + tax +
                    ", finalAmount=" + finalAmount +
                    '}';
        }
    }

    static class Order {

        String id;
        User user;
        Restaurant restaurant;
        List<CartItem> items;
        Bill bill;
        OrderStatus status;

        public Order(String id,
                     User user,
                     Restaurant restaurant,
                     List<CartItem> items,
                     Bill bill) {

            this.id = id;
            this.user = user;
            this.restaurant = restaurant;
            this.items = items;
            this.bill = bill;
            this.status = OrderStatus.CREATED;
        }
    }

    static class DeliveryPartner {

        String id;
        String name;

        List<Order> deliveries = new ArrayList<>();

        public DeliveryPartner(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    // =====================================================
    // COUPON
    // =====================================================

    interface Coupon {
        double apply(double amount);
    }

    static class PercentageCoupon implements Coupon {

        private final double percentage;

        public PercentageCoupon(double percentage) {
            this.percentage = percentage;
        }

        @Override
        public double apply(double amount) {
            return amount * percentage / 100.0;
        }
    }

    // =====================================================
    // PAYMENT STRATEGY
    // =====================================================

    interface PaymentStrategy {
        void pay(double amount);
    }

    static class CardPayment implements PaymentStrategy {

        @Override
        public void pay(double amount) {
            System.out.println("Paid " + amount + " using CARD");
        }
    }

    static class UpiPayment implements PaymentStrategy {

        @Override
        public void pay(double amount) {
            System.out.println("Paid " + amount + " using UPI");
        }
    }

    static class WalletPayment implements PaymentStrategy {

        @Override
        public void pay(double amount) {
            System.out.println("Paid " + amount + " using WALLET");
        }
    }

    // =====================================================
    // SERVICES
    // =====================================================

    static class RestaurantService {

        private final List<Restaurant> restaurants =
                new ArrayList<>();

        void registerRestaurant(Restaurant restaurant) {
            restaurants.add(restaurant);
        }

        List<Restaurant> searchByName(String name) {

            List<Restaurant> result =
                    new ArrayList<>();

            for (Restaurant restaurant : restaurants) {

                if (restaurant.name
                        .toLowerCase()
                        .contains(name.toLowerCase())) {

                    result.add(restaurant);
                }
            }

            return result;
        }

        List<Restaurant> searchByCity(String city) {

            List<Restaurant> result =
                    new ArrayList<>();

            for (Restaurant restaurant : restaurants) {

                if (restaurant.city
                        .equalsIgnoreCase(city)) {

                    result.add(restaurant);
                }
            }

            return result;
        }
    }

    static class BillingService {

        private static final double TAX_PERCENT = 18;

        Bill generateBill(List<CartItem> items,
                          Coupon coupon) {

            double subtotal = 0;

            for (CartItem item : items) {
                subtotal += item.getTotalPrice();
            }

            double discount = 0;

            if (coupon != null) {
                discount = coupon.apply(subtotal);
            }

            double taxableAmount =
                    subtotal - discount;

            double tax =
                    taxableAmount * TAX_PERCENT / 100;

            Bill bill = new Bill();

            bill.subtotal = subtotal;
            bill.discount = discount;
            bill.tax = tax;
            bill.finalAmount =
                    taxableAmount + tax;

            return bill;
        }
    }

    static class OrderService {

        private final BillingService billingService =
                new BillingService();

        private final List<Order> orders =
                new ArrayList<>();

        Order placeOrder(User user,
                         Restaurant restaurant,
                         Coupon coupon,
                         PaymentStrategy paymentStrategy) {

            List<CartItem> items =
                    new ArrayList<>(user.cart.getItems());

            if (items.isEmpty()) {
                throw new RuntimeException(
                        "Cart is empty"
                );
            }

            Bill bill =
                    billingService.generateBill(
                            items,
                            coupon
                    );

            paymentStrategy.pay(
                    bill.finalAmount
            );

            Order order =
                    new Order(
                            UUID.randomUUID().toString(),
                            user,
                            restaurant,
                            items,
                            bill
                    );

            order.status =
                    OrderStatus.CONFIRMED;

            orders.add(order);

            user.cart.clear();

            return order;
        }

        void cancelOrder(Order order) {

            if (order.status ==
                    OrderStatus.DELIVERED) {

                throw new RuntimeException(
                        "Delivered order cannot be cancelled"
                );
            }

            order.status =
                    OrderStatus.CANCELLED;
        }

        List<Order> getOrdersByUser(User user) {

            List<Order> result =
                    new ArrayList<>();

            for (Order order : orders) {

                if (order.user.id.equals(user.id)) {
                    result.add(order);
                }
            }

            return result;
        }

        OrderStatus getStatus(Order order) {
            return order.status;
        }
    }

    // =====================================================
    // DRIVER
    // =====================================================

    public static void main(String[] args) {

        User sachin =
                new User(
                        "U1",
                        "Sachin",
                        "Bangalore"
                );

        Restaurant restaurant =
                new Restaurant(
                        "R1",
                        "Pizza Hub",
                        "Bangalore"
                );

        FoodItem pizza =
                new FoodItem(
                        "F1",
                        "Farmhouse Pizza",
                        400,
                        CuisineType.ITALIAN,
                        MealType.DINNER
                );

        FoodItem pasta =
                new FoodItem(
                        "F2",
                        "White Sauce Pasta",
                        250,
                        CuisineType.ITALIAN,
                        MealType.LUNCH
                );

        restaurant.addFoodItem(pizza);
        restaurant.addFoodItem(pasta);

        RestaurantService restaurantService =
                new RestaurantService();

        restaurantService.registerRestaurant(
                restaurant
        );

        sachin.cart.addItem(
                pizza,
                2
        );

        sachin.cart.addItem(
                pasta,
                1
        );

        Coupon coupon =
                new PercentageCoupon(10);

        PaymentStrategy payment =
                new UpiPayment();

        OrderService orderService =
                new OrderService();

        Order order =
                orderService.placeOrder(
                        sachin,
                        restaurant,
                        coupon,
                        payment
                );

        System.out.println();
        System.out.println("Order Created");
        System.out.println("Order Id : " + order.id);
        System.out.println("Status   : " + order.status);
        System.out.println(order.bill);

        order.status =
                OrderStatus.OUT_FOR_DELIVERY;

        System.out.println();
        System.out.println(
                "Current Status : "
                        + orderService.getStatus(order)
        );

        order.status =
                OrderStatus.DELIVERED;

        System.out.println(
                "Current Status : "
                        + orderService.getStatus(order)
        );
    }
}