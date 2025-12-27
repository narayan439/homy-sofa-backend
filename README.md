# 🛠️ Homy Sofa – Backend (Spring Boot)

This repository contains the **backend service** for the **Homy Sofa Service Booking & Management System**, built using **Spring Boot** and **MySQL**.
It provides REST APIs for managing services, customers, bookings, and admin operations for a sofa cleaning, repair, and renovation business.

---

## 🚀 Features

* Customer service booking **without login**
* Automatic customer creation using phone number
* Auto-generated booking reference (e.g. `HOMY202500001`)
* Admin management APIs for:

  * Services & pricing
  * Service features (✔ dynamic list)
  * Bookings & booking status
  * Customers
* “Most Popular” service support
* Image **path** storage (image files stored on server, not DB)
* Normalized database schema with foreign keys
* RESTful and scalable architecture

---

## 🧱 Tech Stack

* **Backend Framework:** Spring Boot
* **Language:** Java
* **ORM:** Spring Data JPA (Hibernate)
* **Database:** MySQL
* **Build Tool:** Maven
* **API Style:** REST

---

## 🗄️ Database Design

### Main Tables

* `customer` – Stores customer details (auto-created, no login)
* `services` – Stores service info, price, image path, popularity flag
* `service_features` – Stores multiple feature points per service
* `bookings` – Stores booking requests linked to customer & service

### Design Highlights

* Auto-increment internal IDs
* Separate user-friendly booking reference
* Foreign key relationships
* No image blobs stored in database

---

## 🔗 Booking Flow

1. User submits booking form
2. Backend checks customer by phone number
3. If customer does not exist → create new customer
4. Booking is created and linked to customer
5. Booking reference like `HOMY202500001` is generated
6. Response sent to frontend

---

## 📂 Project Structure

```
backend/
 ├── src/main/java/com/homy/backend
 │   ├── controller
 │   ├── service
 │   ├── repository
 │   ├── model
 │   └── config
 ├── src/main/resources
 │   ├── application.properties
 │   └── static/uploads
 └── pom.xml
```

---

## 🖼️ Image Handling

* Images are stored in server folders (e.g. `/uploads/services/`)
* Database stores **only the image path**
* Improves performance and scalability

---

## ⚙️ Setup Instructions

### 1️⃣ Clone the repository

```bash
git clone https://github.com/narayan439/homy-sofa-backend.git
cd homy-sofa-backend
```

### 2️⃣ Configure database

Update `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/customers_and_bookings
spring.datasource.username=root
spring.datasource.password=yourpassword
```

### 3️⃣ Run the application

```bash
mvn spring-boot:run
```

Backend will start at:

```
http://localhost:8080
```

---

## 🎯 Use Case

This backend is suitable for:

* Local service-based businesses
* Full-stack Angular + Spring Boot projects
* Academic final-year projects
* Backend architecture demonstration

---

## 🚀 Future Enhancements

* Authentication & role-based admin access
* Online payment integration
* Email / WhatsApp booking notifications
* Admin analytics & reports
* Cloud image storage

---

## 👨‍💻 Author

**Narayan Sahu**
Computer Science & Engineering
Full Stack Developer (Angular | Spring Boot | MySQL)

---

## 📜 License

This project is for **educational and learning purposes**.
