Feature: An endpoint that returns the details of a book in the library

  As a user
  I want get book endpoint that returns meta data details of a book in my  library
  So that I can confirm if a book is in my library


  Scenario: Looking up a user's book
    Given I am currently authenticated
    When I request a book which exist in my library
    Then the request is successful
    And the correct book is returned
    And the book has the following attributes:
      | attribute       | type       |
      | isbn            | Integer    |
      | title           | String     |
      | author          | String     |
      | sortableTitle   | String     |
      | bookType        | String     |
      | readingStatus   | String     |
      | readingPosition | String     |
      | images          | Collection |
      | addedTime       | DateTime   |


  Scenario: Looking up invalid book with valid
    Given I am currently authenticated
    When I request a book which does not exist in my library
    Then the request fails because it was invalid
    And a valid reason for the failure is returned


  Scenario: Looking up book with invalid user
    Given I am not currently authenticated
    When I request a book which exist in my library
    Then the request fails because it was forbidden



