When(/^I request my library$/) do
  get_library
end

Then(/^the response is a list (?:that is empty|containing (\d+) library items)$/) do |count|
  expect(@response_data['items'].size).to eq(count.to_i)
end
