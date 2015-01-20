Feature: Get a single sample
  As a user
  I want to retrieve a single sample from my library
  So I can view details about my sample library items

  Scenario: Get a single sample library item
    Given I am an authenticated user
    And I have 2 sample library items
    When I request a sample book's details
    Then the request is successful
    And the response contains the following items
      | attribute       | type     |
      | isbn            | String   |
      | title           | String   |
      | author          | String   |
      | sortableAuthor  | String   |
      | ownership       | String   |
      | readingStatus   | String   |
      | readingPosition | Object   |
      | addedDate       | DateTime |
      | images          | Array    |
      | links           | Array    |
    And the ownership status is Sample

  Scenario: Requesting a sample when unauthorized
    Given I am an authenticated user
    And I have 1 sample library item
    When I am not authenticated
    And I request the sample book's details
    Then the request fails because I am unauthorized

  Scenario: Requesting a sample that doesn't exist in my library
    Given I am an authenticated user
    And I have 0 sample library items
    When I request a sample book's details
    Then the request fails because the sample was not found