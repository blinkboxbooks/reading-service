Then(/^a valid reason for the failure is returned$/) do
  expect(@response_data.keys).to eq(%w{code description})
end

Then(/^the error reason is "(.*?)"$/) do |state|
  expect(@response_data['code']).to match(/#{state.gsub(" ", "_")}$/)
end

Then(/^each (\w+) has the following attributes:$/) do |_item, table|
  expected_keys = table.rows.map { |r| r[0] }
  table.rows.each do |attribute|
    @response_data['items'].each do |i|
      expect(i.keys).to match_array(expected_keys)
      if (attribute[1] == "DateTime")
        expect(DateTime.parse(i[attribute[0]])).to be_a(DateTime)
      else
        expect(i[attribute[0]]).to be_a(attribute[1].constantize)
      end
    end
  end
end

Then(/^each (\w+) includes the following attributes:$/) do |_item, table|
  table.rows.each do |attribute|
    @response_data['items'].each do |i|
      expect(i.keys).to include(attribute[0])
      if (attribute[1] == "DateTime")
        expect(DateTime.parse(i[attribute[0]])).to be_a(DateTime)
      else
        expect(i[attribute[0]]).to be_a(attribute[1].constantize)
      end
    end
  end
end

Then(/^the (\w+) has the following attributes:$/) do |_item, table|
  expected_keys = table.rows.map { |r| r[0] }
  table.rows.each do |attribute|
    expect(@response_data.keys).to match_array(expected_keys)
    if (attribute[1] == "DateTime")
      expect(DateTime.parse(i[attribute[0]])).to be_a(DateTime)
    else
      expect(@response_data[attribute[0]]).to be_a(attribute[1].constantize)
    end
  end
end

Then(/^the (\w+) includes the following attributes:$/) do |_item, table|
  table.rows.each do |attribute|
    expect(@response_data.keys).to include(attribute[0])
    if (attribute[1] == "DateTime")
      expect(DateTime.parse(i[attribute[0]])).to be_a(DateTime)
    else
      expect(@response_data[attribute[0]]).to be_a(attribute[1].constantize)
    end
  end
end

Then(/^it has the following images:$/) do |table|
  rels = table.hashes.map { |row| row["relationship"] }
  validate_images(@response_data, *rels)
end

Then(/^each (?:.+) has the following images:$/) do |table|
  rels = table.hashes.map { |row| row["relationship"] }
  @response_data["items"].each do |item|
    validate_images(item, *rels)
  end
end

Then(/^it has the following links:$/) do |table|
  validate_links(@response_data, *table.hashes)
end

Then(/^each (?:.+) has the following links:$/) do |table|
  @response_data["items"].each do |item|
    validate_links(item, *table.hashes)
  end
end

Then(/^the request was successful and an empty response is returned$/) do
  Cucumber::Rest::Status.ensure_status(204)
  expect(HttpCapture::RESPONSES.last.body).to be_empty
end
