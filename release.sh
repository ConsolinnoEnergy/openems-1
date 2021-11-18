#!/bin/bash


function show_usage(){
    printf "WRONG INPUT\n"
    printf "Usgage: $0 [experimental/release]\n"
return 0
}
function update_version(){
    echo ---------------------------------------------------------
    echo Updating Changelog
    echo ---------------------------------------------------------
    nano debian/changelog



    #get changelog Version
    VERSION=$(dpkg-parsechangelog --show-field Version | cut -d'-' -f 1)
    PACKVERS=$(dpkg-parsechangelog --show-field Version | cut -d'-' -f 2)
    PACKNAME=$(dpkg-parsechangelog --show-field Source)


    #get changelog Distribution
    DISTTYPE=$(dpkg-parsechangelog --show-field Distribution)

    # check if tag exists already
    RETVAL=$(git tag -l release/$VERSION-$PACKVERS)
    if [[ -n $RETVAL ]]
    then
    echo ---------------------------------------------------------
    echo $VERSION-$PACKVERS already exists! update changelog and restart script
    echo ---------------------------------------------------------
    exit 1
    fi


    echo ---------------------------------------------------------
    echo Creating RELEASE Package $PACKNAME $VERSION-$PACKVERS
    echo ---------------------------------------------------------


    #make changelog.gz
    gzip -k -f -9 "${PWD}/debian/changelog"

    echo "changelog.gz created"

}
function git_tag(){

    #get changelog Version
    VERSION=$(dpkg-parsechangelog --show-field Version | cut -d'-' -f 1)
    PACKVERS=$(dpkg-parsechangelog --show-field Version | cut -d'-' -f 2)
    PACKNAMETAG=$(dpkg-parsechangelog --show-field Source | cut -d '-' -f 2)

    GITTAG=$PACKNAMETAG/release/$VERSION-$PACKVERS

    echo "Creating git tag: $GITTAG"

   # Automatic  Push deactivated

    #add commit changelog update
    git add -A
    git commit -m packed_$VERSION-$PACKVERS
    #git push origin


    # create release tag
    git tag $GITTAG
    #git push origin $GITTAG



}

function git_push(){

    #get changelog Version
    VERSION=$(dpkg-parsechangelog --show-field Version | cut -d'-' -f 1)
    PACKVERS=$(dpkg-parsechangelog --show-field Version | cut -d'-' -f 2)
    PACKNAMETAG=$(dpkg-parsechangelog --show-field Source | cut -d '-' -f 2)

    GITTAG=$PACKNAMETAG/release/$VERSION-$PACKV


    echo ---------------------------------------------------------
    echo Trigering Pipeline for $GITTAG build!
    echo ---------------------------------------------------------

    git push origin
    git push origin $GITTAG



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

    elif [[ "$1" == "update" ]]; then
        update_version

    elif [[ "$1" == "tag" ]]; then
        git_tag

    elif [[ "$1" == "push" ]]; then
        git_push



    else
        echo "incorrect Input"
        show_usage
    fi
shift
done
