earn Java, Spring Boot, CRUD operations, and H2 database.

Features:

1. Create Bank Account (customerName, email, accountType, initialDeposit)

2. Get Account Details (/accounts/{accNo})

3. Deposit Amount (/accounts/{accNo}/deposit)

4. Withdraw Amount (/accounts/{accNo}/withdraw)

5. Delete Account

PART 2-Loan Calculator Module

Objective:

Calculate EMI, total interest, and total payment.

Features:

1. EMI Calculation (/loan/calculate)

Inputs: principalAmount, annualInterest Rate, tenureInMonths

Outputs: monthlyEMI, totalInterest Payable, totalPayment

Formula:

EMI = [P x R x (1+R)^]/[(1+R)^N-1]

Where:

P = principal

R = monthly rate

N = tenure

2. Loan Summary (/loan/summary)

Returns EMI, total payment, breakdown

Technical Requirements:

- Java 17+

Spring Boot

- H2 DB

- JPA

- Postman

Deliverables:

- GitHub repo

- README

Postman collection
