module KnowsAboutBookDetailsAPI
  def get_customer_voucher_usage_for(customer)
    # to-do http_get :consumer_api, "admin/gifting/vouchers?userId=#{customer.url_encode}", "Accept" => "application/vnd.blinkbox.books.v2+json"
    @response_data = parse_last_api_response
  end
end

World(KnowsAboutBookDetailsAPI)
