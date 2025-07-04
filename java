import java.util.*;

abstract class Product {
    String name;
    double price;
    int quantity;

    Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    abstract boolean isExpired();

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getAvailableQuantity() {
        return quantity;
    }

    public void reduceQuantity(int amount) {
        this.quantity -= amount;
    }
}

interface Shippable {
    String getName();
    double getWeight();
}

class ExpirableProduct extends Product {
    Date expiryDate;

    ExpirableProduct(String name, double price, int quantity, Date expiryDate) {
        super(name, price, quantity);
        this.expiryDate = expiryDate;
    }

    @Override
    boolean isExpired() {
        return new Date().after(expiryDate);
    }
}

class NonExpirableProduct extends Product {
    NonExpirableProduct(String name, double price, int quantity) {
        super(name, price, quantity);
    }

    @Override
    boolean isExpired() {
        return false;
    }
}

class ShippableExpirableProduct extends ExpirableProduct implements Shippable {
    double weight;

    ShippableExpirableProduct(String name, double price, int quantity, Date expiryDate, double weight) {
        super(name, price, quantity, expiryDate);
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}

class ShippableNonExpirableProduct extends NonExpirableProduct implements Shippable {
    double weight;

    ShippableNonExpirableProduct(String name, double price, int quantity, double weight) {
        super(name, price, quantity);
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}

class CartItem {
    Product product;
    int quantity;

    CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
}

class Cart {
    List<CartItem> items = new ArrayList<>();

    public void add(Product product, int quantity) {
        if (quantity > product.getAvailableQuantity()) {
            throw new IllegalArgumentException("Not enough quantity in stock");
        }
        items.add(new CartItem(product, quantity));
    }

    public List<CartItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}

class Customer {
    String name;
    double balance;

    Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public void pay(double amount) {
        if (balance < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        balance -= amount;
    }

    public double getBalance() {
        return balance;
    }
}

class ShippingService {
    public void ship(List<Shippable> items) {
        System.out.println("\n** Shipment notice **");
        Map<String, Double> productWeights = new LinkedHashMap<>();
        double totalWeight = 0;
        for (Shippable item : items) {
            productWeights.put(item.getName(),
                productWeights.getOrDefault(item.getName(), 0.0) + item.getWeight());
            totalWeight += item.getWeight();
        }
        for (Map.Entry<String, Double> entry : productWeights.entrySet()) {
            System.out.printf("1x %-12s %.0fg\n", entry.getKey(), entry.getValue() * 1000);
        }
        System.out.printf("Total package weight %.1fkg\n", totalWeight);
    }
}

class CheckoutService {
    static final double SHIPPING_FEE = 30.0;

    public static void checkout(Customer customer, Cart cart) {
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        double subtotal = 0;
        List<Shippable> shippableItems = new ArrayList<>();
        boolean hasScratchCard = false;
        List<CartItem> tvItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            Product product = item.product;
            int quantity = item.quantity;

            if (product.isExpired()) {
                throw new IllegalArgumentException("Product " + product.getName() + " is expired");
            }

            if (quantity > product.getAvailableQuantity()) {
                throw new IllegalArgumentException("Product " + product.getName() + " is out of stock");
            }

            product.reduceQuantity(quantity);
            subtotal += product.getPrice() * quantity;

            if (product.getName().equalsIgnoreCase("ScratchCard")) {
                hasScratchCard = true;
            }

            if (product instanceof Shippable) {
                for (int i = 0; i < quantity; i++) {
                    shippableItems.add((Shippable) product);
                }
            }

            if (product.getName().equalsIgnoreCase("TV")) {
                tvItems.add(item);
            }
        }

        if (hasScratchCard && !tvItems.isEmpty()) {
            for (CartItem item : tvItems) {
                for (int i = 0; i < item.quantity; i++) {
                    shippableItems.add(new Shippable() {
                        public String getName() { return "TV"; }
                        public double getWeight() { return 5.0; }
                    });
                }
            }
        }

        double total = subtotal + SHIPPING_FEE;
        customer.pay(total);

        if (!shippableItems.isEmpty()) {
            new ShippingService().ship(shippableItems);
        }

        System.out.println("\n** Checkout receipt **");
        for (CartItem item : cart.getItems()) {
            double itemTotal = item.quantity * item.product.getPrice();
            System.out.printf("%dx %-12s %.0f\n", item.quantity, item.product.getName(), itemTotal);
        }
        System.out.println("----------------------");
        System.out.printf("Order subtotal   %.0f EGP\n", subtotal);
        System.out.printf("Shipping fees    %.0f EGP\n", SHIPPING_FEE);
        System.out.printf("Paid amount      %.0f EGP\n", total);
        System.out.printf("Balance after payment: %.0f EGP\n", customer.getBalance());
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        Date tomorrow = cal.getTime();

        Map<String, Product> products = new LinkedHashMap<>();
        products.put("cheese", new ShippableExpirableProduct("Cheese", 100, 5, tomorrow, 0.2));
        products.put("biscuits", new ShippableExpirableProduct("Biscuits", 150, 3, tomorrow, 0.7));
        products.put("tv", new NonExpirableProduct("TV", 3000, 2));
        products.put("scratchcard", new NonExpirableProduct("ScratchCard", 50, 10));

        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        System.out.print("Enter your starting balance: ");
        double balance = scanner.nextDouble();
        scanner.nextLine();

        Customer customer = new Customer(name, balance);
        Cart cart = new Cart();

        System.out.println("\nAvailable products:");
        for (Product p : products.values()) {
            System.out.printf("- %s (%.0f EGP, stock: %d, %s)\n",
                p.getName(), p.getPrice(), p.getAvailableQuantity(),
                (p instanceof Shippable ? "shippable" : "non-shippable"));
        }

        while (true) {
            System.out.print("\nEnter product name to add to cart (or 'done'): ");
            String input = scanner.nextLine().toLowerCase();

            if (input.equals("done")) break;

            Product selected = products.get(input);
            if (selected == null) {
                System.out.println("Product not found.");
                continue;
            }

            System.out.print("Enter quantity: ");
            int qty = scanner.nextInt();
            scanner.nextLine();

            try {
                cart.add(selected, qty);
                System.out.println("Added to cart.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }

        try {
            CheckoutService.checkout(customer, cart);
        } catch (IllegalArgumentException e) {
            System.out.println("\nCheckout failed: " + e.getMessage());
        }
    }
}
