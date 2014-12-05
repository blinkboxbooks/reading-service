Given(/^I have (no|#{CAPTURE_INTEGER}) (?:(purchased|sample|archived|deleted) )?library items? in my library$/) do |count, type|
  # We are ignoring this as we are using data that is preloaded into the database.
  # unless count == "no"
  #   library_items = [*data_for_a(:book, which: "is currently available for purchase", instances: count)]
  #   end
end

When(/^I request my library$/) do
  get_library
end

# A book that works: 9780297859406

When(/I request a library item( which does not exist in my library)?$/) do |missing|
  isbn = missing ? 9780297859406 : 9780007197545
  get_library_item(isbn)
end

Then(/^the response is a list (?:that is empty|containing at least (#{CAPTURE_INTEGER}) library items)$/) do |count|
  count ||= 0
  @response_data['items'] == count
end

