Given(/^I have a customer id who I want to give a book to$/) do
  register_a_new_user!
end

When(/^I add a book with a valid ISBN$/) do
  book = data_for_a(:book, which: "is currently available for purchase")
  add_book_to_library(@user_id, book.merge("ownership" => "Owned"))
end