module KnowsAboutLibraryAPI

  @request_type = "application/vnd.blinkbox.books.v2+json"

  def get_library
    http_get :consumer_api, "/my/library", "Accept" => @request_type
    @response_data = parse_last_api_response
  end

  def get_library_item(isbn)
    http_get :consumer_api, "/my/library/#{isbn}", "Accept" => @request_type
    @response_data = parse_last_api_response
  end

  def add_sample(isbn)
    http_post :consumer_api, "/my/library/samples/", {"isbn" => isbn}, "Accept" => @request_type
    @response_data = parse_last_api_response
  end
end

World(KnowsAboutLibraryAPI)
