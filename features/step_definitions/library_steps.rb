And(/^I have (\d+)( sample)? library item(?:s)?$/) do |count, is_sample|
  @books = data_for_a(:book, which: "is currently available as sample", instances: count.to_i)
  @samples = []
  @books.each do |b|
    @samples << b
    add_sample(b)
    Cucumber::Rest::Status.ensure_status_class(:success)
  end
end