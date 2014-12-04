When(/^I request my library$/) do
  get_library
end

And(/^I have (no|#{CAPTURE_INTEGER}) (?:(purchased|sample|archived|deleted) )?library items? in my library$/) do |count, type|
  unless count == "no"
    library_items = [*data_for_a(:book, which: "is currently available for purchase", instances: count)]
    end
end

And(/^the response is a list (?:which is empty |containing at least #{CAPTURE_INTEGER} library items)$/) do |count|

end