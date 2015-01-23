# Change log

## 0.8.7 ([#39](https://git.mobcastdev.com/Agora/reading-service/pull/39) 2015-01-23 10:45:07)

Remove body from Add Sample responses

### Bugfix

- Removed bodies from "Add Sample" endpoint responses (http://jira.blinkbox.local/jira/browse/PT-570).

## 0.8.6 ([#38](https://git.mobcastdev.com/Agora/reading-service/pull/38) 2015-01-22 17:59:08)

Location headers

### Bugfix

- Added `Location` header to the response of `POST /my/library/samples/`.

## 0.8.5 ([#37](https://git.mobcastdev.com/Agora/reading-service/pull/37) 2015-01-20 14:12:53)

PT-372: PT-402 automated tests for get sample

patch 

- PT-372: PT-402 automated tests for get sample

## 0.8.4 ([#33](https://git.mobcastdev.com/Agora/reading-service/pull/33) 2015-01-14 16:44:38)

Get library - basic details

Test improvement

Tests for http://jira.blinkbox.local/jira/browse/PT-376

Currently only for book samples, will add tests for full books and mixed sample/full library once implementing add book tests. 

## 0.8.3 ([#34](https://git.mobcastdev.com/Agora/reading-service/pull/34) 2015-01-14 11:42:25)

Fix typo

### Patch

- Fixed typo in spec

## 0.8.2 ([#32](https://git.mobcastdev.com/Agora/reading-service/pull/32) 2015-01-12 10:40:07)

Add health endpoints to public service

### Improvement

- Health endpoints for public API.

## 0.8.1 ([#31](https://git.mobcastdev.com/Agora/reading-service/pull/31) 2015-01-07 16:29:25)

Test data improvements

### Improvement

- Refactored test code to build expected JSON rather use hardcoded JSON strings.

## 0.8.0 ([#30](https://git.mobcastdev.com/Agora/reading-service/pull/30) 2015-01-06 09:24:37)

V2 errors

### New Feature

- Support for v2 API Error responses.

## 0.7.4 ([#29](https://git.mobcastdev.com/Agora/reading-service/pull/29) 2014-12-09 15:56:47)

Core automation

Test framework improvement

Adding base of automation part of project along with first feature file.

## 0.7.3 ([#28](https://git.mobcastdev.com/Agora/reading-service/pull/28) 2014-12-05 15:38:40)

Bulk media stubbing

### Patch

## 0.7.2 ([#27](https://git.mobcastdev.com/Agora/reading-service/pull/27) 2014-12-04 13:53:47)

Added stubbing for returning fake links

A patch to adding stubbing for media links.
- The reason for this is to make testing easier for the time being

## 0.7.1 ([#26](https://git.mobcastdev.com/Agora/reading-service/pull/26) 2014-12-04 13:30:16)

Delete sample yaml

A patch to update the yaml file to include the delete sample endpoint.

## 0.7.0 ([#24](https://git.mobcastdev.com/Agora/reading-service/pull/24) 2014-12-03 15:25:23)

Add sample

New Feature: Adding and retrieving samples!

## 0.6.0 ([#23](https://git.mobcastdev.com/Agora/reading-service/pull/23) 2014-12-02 17:36:20)

Admin add book

### New feature

- `Add full book` admin endpoint.

## 0.5.1 ([#22](https://git.mobcastdev.com/Agora/reading-service/pull/22) 2014-11-28 18:47:09)

Fix catalogue url

A patch to fix the url that calls the catalogue service.

## 0.5.0 ([#21](https://git.mobcastdev.com/Agora/reading-service/pull/21) 2014-11-28 10:45:18)

Add full book endpoint spec

### New feature

- Add full book endpoint spec.

## 0.4.0 ([#20](https://git.mobcastdev.com/Agora/reading-service/pull/20) 2014-11-26 14:53:22)

Added health endpoint

### New feature

- Added health endpoint

## 0.3.0 ([#17](https://git.mobcastdev.com/Agora/reading-service/pull/17) 2014-11-26 14:33:04)

Get library

A new feature - the get library endpoint

## 0.2.0 ([#18](https://git.mobcastdev.com/Agora/reading-service/pull/18) 2014-11-26 13:39:20)

Walking skeleton

### New Feature

Walking skeleton for admin API.

## 0.1.4 ([#15](https://git.mobcastdev.com/Agora/reading-service/pull/15) 2014-11-19 17:59:41)

Spec update

### Bugfix

- Removed sortable title
- Added sortable author

## 0.1.3 ([#14](https://git.mobcastdev.com/Agora/reading-service/pull/14) 2014-11-19 14:46:34)

500 responses are now JSON

### Improvement

- `500 Internal Error` responses now return `Error` as JSON.

## 0.1.2 ([#13](https://git.mobcastdev.com/Agora/reading-service/pull/13) 2014-11-17 17:23:40)

DB schemas

### Improvement

- Added Database schemas
- Changed enum representation in DB to integers

## 0.1.1 ([#12](https://git.mobcastdev.com/Agora/reading-service/pull/12) 2014-11-17 15:58:29)

Adding empty config files to admin project to fix the TeamCity build

### Bugfix

- Fixed `admin` build on TeamCity.

## 0.1.0 ([#11](https://git.mobcastdev.com/Agora/reading-service/pull/11) 2014-11-17 14:37:43)

Get book details 

### New feature

Get book details endpoint

