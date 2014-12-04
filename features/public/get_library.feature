Feature: Get library details

  As a user
  I want one API call to retrieve my library the first time I open my app
  So that I can have access to my library and books

  Scenario: Using a valid user with one or more books
    Given I am authenticated as a user
    And I have one book in my library
    When I request my library
    Then the request is successful
    And the response is a list containing at least one library item

  Scenario: Using a valid user with no books
    Given I am authenticated as a user
    And I have no books in my library
    When I request my library
    Then the request is successful
    And the response is a list which is empty