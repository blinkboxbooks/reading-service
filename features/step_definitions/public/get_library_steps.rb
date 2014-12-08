When(/^I request my library$/) do
  get_library
end

When(/I request a library item( which does not exist in my library)?$/) do |missing|
  isbn = missing ? 9780297859406 : 9780007197545
  get_library_item(isbn)
end

Then(/^the response is a list (?:that is empty|containing at least (#{CAPTURE_INTEGER}) library items)$/) do |count|
  count ||= 0
  expect(@response_data['items'].size).to eq(count)
end

