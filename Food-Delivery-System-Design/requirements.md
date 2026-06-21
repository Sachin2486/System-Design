Problem Statement: Design a Food Delivery System

Design and implement an in-memory Food Delivery Application similar to Zomato, Swiggy, or UberEats.

The system should support the following functionalities:

User Management
Create a user profile.
Retrieve user details.
Each user should have a shopping cart.
Restaurant Management
Restaurants should be able to register themselves.
A restaurant can maintain a menu consisting of multiple food items.
Each food item should contain:
Name
Price
Cuisine Type
Meal Type
Restaurant Search

Users should be able to search restaurants:

By restaurant name.
By city.
Menu Browsing

Users should be able to view food items offered by a restaurant.

Food items should support categorization based on:

Cuisine Type (Indian, Chinese, Italian, etc.)
Meal Type (Breakfast, Lunch, Dinner, etc.)
Cart Management

Users should be able to:

Add food items to cart.
Remove food items from cart.
View all cart items.

Assume that a cart can contain food items from only one restaurant at a time.

Coupon Support

The system should support applying coupons on an order.

For example:

10% OFF
20% OFF

Coupon logic should be extensible so that new coupon types can be added easily in the future.

Bill Generation

The system should generate a detailed bill containing:

Item subtotal
Discount amount
Tax amount
Final payable amount

Assume tax percentage is configurable.

Payment

Users should be able to pay using multiple payment methods such as:

Credit/Debit Card
UPI
Wallet

Payment logic should be easily extensible to support new payment methods.

Order Management

Users should be able to:

Place an order.
Cancel an order.
View all orders placed by them.

Each order should maintain its current status.

Order Tracking

The system should support the following order lifecycle:

CREATED
CONFIRMED
OUT_FOR_DELIVERY
DELIVERED
CANCELLED

Users should be able to track the current status of an order.

Delivery Partner

The system should support delivery partners.

Each delivery partner should maintain a list of deliveries assigned to them.

Users should be able to view the delivery status of their order.