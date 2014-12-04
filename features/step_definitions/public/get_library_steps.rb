When(/^I request my library$/) do
  get_library
end

And(/^I have (no|#{CAPTURE_INTEGER}) (?:(purchased|sample|archived|deleted) )?books? in my library$/) do |count, type|
  unless count == "no"
    books = [*data_for_a(:book, which: "is currently available for purchase", instances: count)]
    end
end

And(/^the response is a list which is empty$/) do
  pending
end