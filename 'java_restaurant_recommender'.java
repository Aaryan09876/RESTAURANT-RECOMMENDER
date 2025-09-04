/*
 * Java Restaurant Recommender
 * Single-file console application.
 * Features:
 *  - Preloaded sample restaurants
 *  - Ask user preferences (cuisine, max price, min rating, distance preference)
 *  - Simple scoring algorithm to rank restaurants
 *  - Show top N recommendations
 *  - Allow adding a new restaurant at runtime
 *
 * To run: javac RestaurantRecommender.java && java RestaurantRecommender
 */

import java.util.*;
import java.util.stream.Collectors;

class Restaurant {
    String name;
    String cuisine; // e.g., "Indian", "Italian"
    double rating; // 0.0 - 5.0
    int priceLevel; // 1 (cheap) - 4 (expensive)
    double distanceKm; // approximate distance

    Restaurant(String name, String cuisine, double rating, int priceLevel, double distanceKm) {
        this.name = name;
        this.cuisine = cuisine;
        this.rating = rating;
        this.priceLevel = priceLevel;
        this.distanceKm = distanceKm;
    }

    @Override
    public String toString() {
        return String.format("%s — %s | Rating: %.1f | Price: %d | %.1f km", name, cuisine, rating, priceLevel, distanceKm);
    }
}

public class RestaurantRecommender {
    private static final Scanner scanner = new Scanner(System.in);
    private final List<Restaurant> restaurants = new ArrayList<>();

    public RestaurantRecommender() {
        seedSampleRestaurants();
    }

    private void seedSampleRestaurants() {
        // A small sample dataset. Expand as you like.
        restaurants.add(new Restaurant("The Spice House", "Indian", 4.5, 2, 1.2));
        restaurants.add(new Restaurant("Mama Mia Pizzeria", "Italian", 4.2, 2, 3.5));
        restaurants.add(new Restaurant("Sushi World", "Japanese", 4.7, 3, 6.0));
        restaurants.add(new Restaurant("Green Garden", "Vegan", 4.0, 1, 2.1));
        restaurants.add(new Restaurant("Burger Barn", "American", 3.9, 1, 0.8));
        restaurants.add(new Restaurant("Curry Corner", "Indian", 4.1, 1, 5.2));
        restaurants.add(new Restaurant("Le Bistro", "French", 4.6, 4, 8.0));
        restaurants.add(new Restaurant("Noodle House", "Chinese", 4.0, 1, 4.0));
    }

    private void run() {
        System.out.println("Welcome to the Java Restaurant Recommender!");

        while (true) {
            System.out.println();
            System.out.println("Choose an option:");
            System.out.println("1) Get recommendations");
            System.out.println("2) Add a restaurant");
            System.out.println("3) List all restaurants");
            System.out.println("4) Exit");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": getRecommendations(); break;
                case "2": addRestaurant(); break;
                case "3": listRestaurants(); break;
                case "4": System.out.println("Goodbye!"); return;
                default: System.out.println("Invalid choice — try again.");
            }
        }
    }

    private void listRestaurants() {
        System.out.println("\nAll restaurants:");
        for (int i = 0; i < restaurants.size(); i++) {
            System.out.printf("%d) %s\n", i + 1, restaurants.get(i));
        }
    }

    private void addRestaurant() {
        System.out.print("Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Cuisine: ");
        String cuisine = scanner.nextLine().trim();
        double rating = readDouble("Rating (0.0 - 5.0): ", 0.0, 5.0);
        int price = (int)readDouble("Price level (1 cheap - 4 expensive): ", 1, 4);
        double distance = readDouble("Distance in km: ", 0, 200);

        restaurants.add(new Restaurant(name, cuisine, rating, price, distance));
        System.out.println("Restaurant added!");
    }

    private double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                double v = Double.parseDouble(line);
                if (v < min || v > max) {
                    System.out.printf("Value must be between %.1f and %.1f.\n", min, max);
                } else return v;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private void getRecommendations() {
        System.out.print("Preferred cuisine (leave blank for any): ");
        String prefCuisine = scanner.nextLine().trim();
        double maxPrice = readDouble("Maximum price level (1-4, enter 4 for no strict limit): ", 1, 4);
        double minRating = readDouble("Minimum rating (0.0 - 5.0): ", 0.0, 5.0);
        double maxDistance = readDouble("Maximum distance in km (enter 100 for no strict limit): ", 0, 200);
        int topN = (int)readDouble("How many top recommendations to show? ", 1, 20);

        // Compute a simple score for each restaurant
        Map<Restaurant, Double> scoreMap = new HashMap<>();
        for (Restaurant r : restaurants) {
            double score = scoreRestaurant(r, prefCuisine, maxPrice, minRating, maxDistance);
            scoreMap.put(r, score);
        }

        // Sort by score descending and filter out zero scores
        List<Map.Entry<Restaurant, Double>> sorted = scoreMap.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            System.out.println("No restaurants match your criteria.");
            return;
        }

        System.out.println("\nTop recommendations:");
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            Map.Entry<Restaurant, Double> e = sorted.get(i);
            System.out.printf("%d) %s (score: %.3f)\n", i + 1, e.getKey(), e.getValue());
        }
    }

    private double scoreRestaurant(Restaurant r, String prefCuisine, double maxPrice, double minRating, double maxDistance) {
        // Early rejection
        if (r.rating < minRating) return 0.0;
        if (r.priceLevel > maxPrice) return 0.0;
        if (r.distanceKm > maxDistance) return 0.0;

        // Base score from rating
        double score = r.rating / 5.0; // 0.0 - 1.0

        // Prefer matching cuisine (boost)
        if (!prefCuisine.isEmpty()) {
            String lowerPref = prefCuisine.toLowerCase();
            if (r.cuisine.toLowerCase().contains(lowerPref)) score += 0.35; // boost
            else score += 0.0;
        }

        // Prefer closer restaurants (distance contribution)
        // We'll map distance 0..maxDistance to 0.3..0.0 contribution (closer = more points)
        double distContribution = Math.max(0, (maxDistance - r.distanceKm) / Math.max(1.0, maxDistance)) * 0.3;
        score += distContribution;

        // Prefer cheaper restaurants slightly
        double priceContribution = (4 - r.priceLevel) / 3.0 * 0.15; // ranges roughly 0..0.15
        score += priceContribution;

        return score; // higher is better
    }

    public static void main(String[] args) {
        new RestaurantRecommender().run();
    }
}
