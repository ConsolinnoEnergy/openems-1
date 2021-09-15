#!/bin/bash


function show_usage(){
    printf "WRONG INPUT\n"
    printf "Usgage: $0 [experimental/release]\n"
return 0
}

function upload_pack(){
PACKAGENAME=$(ls build/distributions/*deb)
PACKAGEPATH=$PWD/$PACKAGENAME

echo $PACKAGEPATH
echo 
echo ---------------------------------------------------------
echo "Uploading package: $PACKAGENAME"
echo ---------------------------------------------------------


curl -u $REPO_USERNAME:$REPO_PASSWORD -X POST -F file=@$PACKAGEPATH "$REPO_ADRESS/api/files/upload"


echo ---------------------------------------------------------
echo "Update Repo from Upload Folder"
echo ---------------------------------------------------------
curl -u $REPO_USERNAME:$REPO_PASSWORD -X POST $REPO_ADRESS/api/repos/$REPO_NAME/file/upload
echo 
echo ---------------------------------------------------------
echo "Publish repo"
echo ---------------------------------------------------------
curl -u $REPO_USERNAME:$REPO_PASSWORD -X PUT -H 'Content-Type: application/json' --data '{"Signing":{"Batch": true,"Passphrase": "'"$PACKAGE_SIGN_PASSPHRASE"'"}}' $REPO_ADRESS/api/publish/:./buster  
}




if [[ "$1" == "" ]]; then
    show_usage
fi

while [ ! -z "$1" ]; do
    if [[ "$1" == "--help" ]] || [[ "$1" == "-h" ]]; then
        show_usage
        
    elif [[ "$1" == "upload" ]]; then
        upload_pack

    
    else
        echo "incorrect Input"
        show_usage
    fi
shift
done
