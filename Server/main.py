import json

import uvicorn
from fastapi import FastAPI, Request
import orjson
import firebase_admin
from firebase_admin import credentials, db, firestore
from google.cloud.firestore import GeoPoint

# Use Firebase Real Time db
cred = credentials.Certificate("serviceAccountKey.json")

firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://giveandtake-31249-default-rtdb.firebaseio.com'
})

# Use Cloud Firestore db
credFs = credentials.Certificate('serviceAccountKey.json')

dbFs = firestore.client()

app = FastAPI()

#remember to connect to Wireless LAN adapter Wi-Fi:  IPv4 Address. . . . . . . . . . . : 10.0.0.3 for example
#and connect your phone to the same Wi-Fi as the PC server and app

@app.get("/")
async def root():
    return "Hello World, Give And Take server is responding."

@app.post('/postRequest/')
async def submit(request: Request):
    print('enter /postRequest')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    isManager = data['isManager']
    requestId = data['requestId']
    subject = data['subject']
    contactDetails= data['contactDetails']
    locationLat= data['locationLat']
    locationLang= data['locationLang']
    creationTime= data['creationTime']
    body= data['body']
    print(userId)
    users_ref = db.reference('users/')
    request_ref = users_ref.child(userId).child("requestId").child(requestId)
    request_ref.update({
        'subject': subject,
        'contactDetails': contactDetails,
        'body': body,
        'creationTime': creationTime
    })
    request_location_ref= request_ref.child('location')
    request_location_ref.update({
        'latitude': float(locationLat),
        'longitude': float(locationLang)
    })

    # add marker to cloud FS
    data= {}
    data["geoPoint"]= GeoPoint(float(locationLat), float(locationLang))
    data["requestId"] = requestId
    data["userId"]= userId
    data["isManager"] = isManager
    data["creationTime"] = creationTime
    dbFs.collection("MapsData").add(data)

    return 'success'


@app.post('/delete/')
async def delete(request: Request):
    print("am deleting")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    docId= data['docId']
    users_ref = db.reference('users/')
    joiners_list= users_ref.child(userId).child("requestId").child(requestId).child(
            "joiners")
    joiners_list = joiners_list.get(False, True)  # as a dict
    request_to_delete_ref= users_ref.child(userId).child("requestId").child(requestId)
    request_to_delete_ref.delete()

    db.reference('reportedRequests').child(requestId).delete()
    #remove from all joiners list
    if joiners_list != None:
        for joinerId in joiners_list:
            print(joinerId)
            db.reference("reportedRequests").child(requestId).delete()
            users_ref.child(joinerId).child("requestsUserJoined").child(requestId).delete()

    # deletes marker from markersDb Cloud Fire Store
    dbFs.collection("MapsData").document(docId).delete()
    
    return 'success'

@app.post('/report/')
async def report(request: Request):
    body = await request.body()
    print("am reporting")
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestUserId= data['requestUserId']
    requestId = data['requestId']
    print('userId:'+userId+",requestUserId:"+requestUserId+",requestId:"+requestId)
    users_ref = db.reference('users/')
    fullName= users_ref.child(userId).child('fullName').get()
    reported_requests_ref = db.reference('reportedRequests/')
    report_ref= reported_requests_ref.child(requestId).child("reporters")
    request_ref= reported_requests_ref.child(requestId)
    report_ref.update({userId: fullName})
    request_ref.update({'requestUserId': requestUserId})
    return 'success'

@app.post('/unReport/')
async def unReport(request: Request):
    print("clicked unreport")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    reported_requests_ref = db.reference('reportedRequests/')
    report_ref= reported_requests_ref.child(requestId).child("reporters")
    request_report_ref= reported_requests_ref.child(requestId)
    report_ref.child(userId).delete()
    if report_ref.get() is None:   #if no users report this anymore, remove request from reported list
       request_report_ref.delete()
       print("request is no longer reported by any user")
    return 'success'

@app.post('/unJoin/')
async def unJoin(request: Request):
    print("enter unjoined")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    requestUserId = data['requestUserId']
    users_ref = db.reference('users/')
    users_ref.child(requestUserId).child("requestId").child(requestId).child("joiners").child(
        userId).delete()
    users_ref.child(userId).child("requestsUserJoined").child(requestId).delete()
    return 'success'

@app.post('/join/')
async def join(request: Request):
    print("enter join")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    requestUserId = data['requestUserId']
    users_ref = db.reference('users/')
    joinerContactDetails = users_ref.child(userId).child('email').get()
    users_ref.child(requestUserId).child("requestId").child(requestId).child("joiners").child(
        userId).child("contactDetails").set(joinerContactDetails)
    users_ref.child(userId).child("requestsUserJoined").child(requestId).child(
        "requestUserId").set(requestUserId)
    return 'success'

@app.post('/getRequestDetails/')
async def getRequestDetails(request: Request):
    print("enter getRequestDetails")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    requestUserId = data['requestUserId']
    requestId = data['requestId']
    if "\"" in requestUserId:
        requestUserId= requestUserId[1:]
        requestUserId= requestUserId[:-1]
    print('requestId: '+requestId)
    print('requestUserId: ',requestUserId)
    users_ref = db.reference('users/')
    requestSubject = users_ref.child(requestUserId).child("requestId").child(requestId).child("subject").get()
    requestBody = users_ref.child(requestUserId).child("requestId").child(requestId).child("body").get()
    contactDetails = users_ref.child(requestUserId).child("requestId").child(requestId).child("contactDetails").get()
    requestLatitude = str(users_ref.child(requestUserId).child("requestId").child(requestId).child("location").child("latitude").get())
    requestLongitude = str(users_ref.child(requestUserId).child("requestId").child(requestId).child("location").child("longitude").get())
    creationTime = users_ref.child(requestUserId).child("requestId").child(requestId).child("creationTime").get()
    # jsonString example: "{\"email\": \"example@com\", \"name\": \"John\"}";
    result= {"requestBody": requestBody, "requestSubject": requestSubject, "contactDetails": contactDetails, "requestLongitude": requestLongitude, "requestLatitude": requestLatitude, "creationTime": creationTime}
    print(result)
    result= requestBody+"||##"+ requestSubject+"||##"+ contactDetails+"||##"+ requestLongitude+"||##"+ requestLatitude+"||##"+creationTime
    # Serializing json
    print(result)
    return result


@app.post('/setKmDistanceNotifications/')
async def setKmDistanceNotifications(request: Request):
    print("enter setKmDistanceNotifications")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    distance= data['distance']
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("distance").set(distance);
    return 'success'

@app.post('/turnOnOffAutoDetectLocationNotifications/')
async def turnOnAutoDetectLocationBtn(request: Request):
    print("enter turnOnOffAutoDetectLocationNotifications")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    onOff = int( data['onOff'])
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("autoDetectLocation").set(onOff);
    return 'success'

@app.post('/useSpecificLocationNotifications/')
async def useSpecifiedLocation(request: Request):
    print("enter useSpecifiedLocation")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    latitude = float(data['latitude'])
    longitude = float(data['longitude'])
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("autoDetectLocation").set(0);
    users_ref.child(userId).child("settings").child("notifications").child("specificLocation").child('latitude').set(latitude)
    users_ref.child(userId).child("settings").child("notifications").child("specificLocation").child('longitude').set(longitude)
    return 'success'

@app.post('/turnOnOffNotifications/')
async def turnOnNotifications(request: Request):
    print("enter turnOnOffNotifications")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    onOff = int (data['onOff'])
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("turnedOn").set(onOff);
    return 'success'

@app.post('/useCurrLocationNotifications/')
async def useCurrLocationNotifications(request: Request):
    print("enter turnOnOffNotifications")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    latitude = float(data['latitude'])
    longitude = float(data['longitude'])
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("autoDetectLocation").set(0)
    users_ref.child(userId).child("settings").child("notifications").child("specificLocation").child('latitude').set(latitude)
    users_ref.child(userId).child("settings").child("notifications").child("specificLocation").child('longitude').set(longitude)
    return 'success'

@app.post('/setLastTimeSeenMap/')
async def setLastTimeSeenMap(request: Request):
    print('enter setLastTimeSeenMap')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    time = data['time']
    users_ref = db.reference('users/')
    users_ref.child(userId).child("lastTimeSeenMap").set(time);

@app.post('/getIsNotificationsTurnedOn/')
async def getIsNotificationsTurnedOn(request: Request):
    print('enter getIsNotificationsTurnedOn')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    users_ref = db.reference('users/')
    notificationsTurnedOn = str(users_ref.child(userId).child("settings").child("notifications").child("turnedOn").get())
    return notificationsTurnedOn

@app.post('/blockUnblockUser/')
async def blockUnblockUser(request: Request):
    print('enter getIsNotificationsTurnedOn')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    blockUnblock= data['blockUnblock']
    users_ref = db.reference('users/')
    users_ref.child(userId).child("isBlocked").set(blockUnblock);

@app.post('/getFinal1/')
async def getFinal1(request: Request):
    print('enter getFinal1')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    requestId= data['requestId']
    requestUserId = data['requestUserId']
    users_ref = db.reference('users/')
    finalRequestUserId= users_ref.child(requestUserId).child("requestsUserJoined").child(requestId).child("requestUserId").get()
    print("final: "+finalRequestUserId)
    return finalRequestUserId

@app.post('/getFinal2/')
async def getFinal2(request: Request):
    print('enter getFinal2')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    requestId= data['requestId']
    userId= data['userId']
    users_ref = db.reference('users/')
    finalRequestUserId= users_ref.child(userId).child("requestsUserJoined").child(requestId).child("requestUserId").get()
    return finalRequestUserId

@app.post('/getJoiners/')
async def getJoiners(request: Request):
    print('enter getJoiners')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    requestId= data['requestId']
    requestUserId= data['requestUserId']
    print("requestUserId: "+requestUserId)
    print("userId: "+requestId)
    users_ref = db.reference('users/')
    joiners_list= users_ref.child(requestUserId).child("requestId").child(requestId).child("joiners")
    joiners_list = joiners_list.get(False, True)  # as a dict
    #remove from all joiners list
    joinersInfo = []
    if joiners_list != None:
        for joinerId in joiners_list:
            print(joinerId)
            joinersInfo.append("Name: "+str(users_ref.child(joinerId).child("fullName").get())+" | Phone number: "+str(joinerId)+"||##");
    if not joinersInfo:
        joinersInfo= "TThere are no joiners yet, please come back later!!"
    print(type(joinersInfo))
    return joinersInfo

@app.post('/getUsers/')
async def getUsers(request: Request):
    print('enter getUsers')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    users_ref = db.reference('users/')
    users_list= users_ref.get(False, True)  # as a dict
    #remove from all users list
    usersInfo = []
    if users_list != None:
        for userId in users_list:
            print(userId)
            usersInfo.append("Name: "+str(users_ref.child(userId).child("fullName").get())+" | Phone number: "+str(userId)+"||##");

    if not usersInfo:
        usersInfo= "TThere are no users yet.."
    print(type(usersInfo))
    return usersInfo

@app.post('/getReportedRequests/')
async def getReportedRequests(request: Request):
    print('enter getReportesRequests')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    users_ref = db.reference('users/')
    reports_ref = db.reference('reportedRequests/')
    reports_list = reports_ref.get(False, False)  # as a dict
    #remove from all joiners list
    reportsInfo = []
    if reports_list != None:
        for requestId in reports_list:
            requestUserId= reports_ref.child(requestId).child("requestUserId").get()
            reporters= str(reports_ref.child(requestId).child("reporters").get())
            reporters= "{Phone Number: "+reporters[1:]
            reporters= reporters.replace("=", " | Name: ")
            reporters= reporters.replace(",", ", Phone Number: ")
            print("requestUserId: "+requestUserId)
            print("requestId: "+requestId)
            requestSubject = str(users_ref.child(requestUserId).child("requestId").child(requestId).child("subject").get())
            reportsInfo.append("Subject: " + requestSubject + " | Request Id: " + requestId+" | RequestUserId: " + requestUserId+ " | reporters: "+reporters+"||##")

    if not reportsInfo:
        reportsInfo= "TThere are no reports.."
    print(type(reportsInfo))
    print(reportsInfo)
    return reportsInfo

@app.post('/getOpenRequests/')
async def getOpenRequests(request: Request):
    print('enter getOpenRequests')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    requestUserId= data['requestUserId']
    users_ref = db.reference('users/')
    requests_list = users_ref.child(requestUserId).child("requestId").get()# as a dict
    #remove from all joiners list
    requestsInfo = []
    if requests_list != None:
        for requestId in requests_list:
            requestSubject = users_ref.child(requestUserId).child("requestId").child(requestId).child("subject").get()
            requestsInfo.append("Subject: " + requestSubject + " | Request Id: " + requestId+"||##");
    if not requestsInfo:
        requestsInfo= "TThere are no requests.."
    print(type(requestsInfo))
    print(requestsInfo)
    return requestsInfo

@app.post('/getJoinedRequests/')
async def getJoinedRequests(request: Request):
    print('enter getOpenRequests')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    requestUserId= data['requestUserId']
    users_ref = db.reference('users/')
    requests_list = users_ref.child(requestUserId).child("requestsUserJoined").get()  # as a dict
    requestsInfo = []
    if requests_list != None:
        for requestId in requests_list:
            creatorOfRequestUserJoined = users_ref.child(requestUserId).child("requestsUserJoined").child(requestId).child("requestUserId").get()
            requestSubject = users_ref.child(creatorOfRequestUserJoined).child("requestId").child(requestId).child("subject").get()
            requestsInfo.append("Subject: " + requestSubject + " | Request Id: " + requestId+"||##");
    if not requestsInfo:
        requestsInfo= "TThere are no requests.."
    print(type(requestsInfo))
    print(requestsInfo)
    return requestsInfo

@app.post('/getDoesPhoneExist/')
async def getDoesPhoneExist(request: Request):
    print('enter getDoesPhoneExist')
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    phone= data['phone']
    users_ref = db.reference('users/')
    phone_user = users_ref.child(phone).get()  # as a dict
    boolExist= False
    if phone_user != None:
        boolExist= True
    print(type(boolExist))
    print(boolExist)
    if boolExist:
        return "true"
    else:
        return "false"

if __name__ == "__main__":
    uvicorn.run(app, host="10.0.0.3", port=8000)

