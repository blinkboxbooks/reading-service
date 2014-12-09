module KnowsAboutLibraryAPI

  def get_library
    http_get :consumer_api, "my/library", "Accept" => "application/vnd.blinkbox.books.v2+json"
    @response_data = parse_last_api_response
  end

  def get_library_item(isbn)
    http_get :consumer_api, "my/library/#{isbn}", "Accept" => "application/vnd.blinkbox.books.v2+json"
    @response_data = parse_last_api_response
  end

  def add_sample(isbn)
    http_post :consumer_api, "my/library/samples", isbn, "Content-Type" => "application/vnd.blinkbox.books.v2+json"
    @response_data = parse_last_api_response
  end
end

World(KnowsAboutLibraryAPI)
