Feature: Add book to user's library

  As a Customer Service Representative
  I want to be able to add a book to the customer's library
  So that I can resolve support issues

  Scenario Outline: Adding Full book to user's library
    Given I am authenticated as a user with the <role> role
    And I have a customer id who I want to give a book to
    When I add a book with a valid ISBN
    Then the request is successful

    Examples: Valid roles
      | role  | description                       |
      | csm   | Customer Services Manager         |
      | csr   | Customer Services Representative  |

  Scenario: Adding Full book that was sampled before
    Given I am authenticated as a user with the csr role
    And I have a customer id who I want to give a book to
    And a customer has a sample book in his library
    When I add a Full book that was sampled before
    Then the request is successful
    And the ownership of the book is Owned

  Scenario: Adding Full book when it already exists
    Given I am authenticated as a user with the csr role
    And I have a customer id who I want to give a book to
    And a customer has a Full book in his library
    When I add that same book
    Then the request fails because there is a conflict
