When(/^I add a book with a valid ISBN$/) do
  book = data_for_a(:book, which: "is currently available for purchase")
  add_book_to_library(@user_id, book.merge("ownership" => "Owned"))
end