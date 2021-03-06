/**
 * Copyright (c) 2012 Martin M Reed
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.hardisonbrewing.maven.cxx.xcode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;
import org.hardisonbrewing.maven.core.FileUtils;
import org.hardisonbrewing.maven.core.JoJoMojoImpl;

/**
 * @goal xcode-unlock-keychain
 * @phase generate-resources
 */
public class UnlockKeychainMojo extends JoJoMojoImpl {

    public static final String KEYCHAIN_REGEX = "\"([^\"]*)\"";

    /**
     * @parameter
     */
    public Keychain keychain;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if ( keychain == null ) {
            return;
        }

        String keychainPath = XCodeService.getKeychainPath();
        boolean exists = FileUtils.exists( keychainPath );

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append( "Using keychain path `" );
        stringBuffer.append( keychainPath );
        stringBuffer.append( "`... exists[" );
        stringBuffer.append( exists );
        stringBuffer.append( "]" );
        getLog().info( stringBuffer.toString() );

        if ( !exists ) {
            getLog().info( "Creating new keychain: " + keychainPath );
            createKeychain( keychain, keychainPath );
        }
        else {

            boolean found = false;

            getLog().info( "Checking current keychain search path..." );
            String[] keychains = listKeychains();

            for (String keychain : keychains) {
                if ( keychain.equals( keychainPath ) ) {
                    found = true;
                    break;
                }
            }

            if ( !found ) {
                getLog().info( "Adding keychain to search path: " + keychainPath );
                addKeychain( keychains, keychainPath );
            }
        }

        unlockKeychain( keychain, keychainPath );
    }

    private String[] listKeychains() {

        List<String> cmd = new LinkedList<String>();
        cmd.add( "security" );
        cmd.add( "list-keychains" );

        StringStreamConsumer streamConsumer = new StringStreamConsumer();
        execute( cmd, streamConsumer, streamConsumer );

        List<String> keychains = new ArrayList<String>();

        Pattern pattern = Pattern.compile( KEYCHAIN_REGEX );
        Matcher matcher = pattern.matcher( streamConsumer.getOutput() );

        while (matcher.find()) {
            keychains.add( matcher.group( 1 ) );
        }

        String[] _keychains = new String[keychains.size()];
        keychains.toArray( _keychains );
        return _keychains;
    }

    private void createKeychain( Keychain keychain, String path ) {

        List<String> cmd = new LinkedList<String>();
        cmd.add( "security" );
        cmd.add( "create-keychain" );
        cmd.add( "-p" );
        cmd.add( keychain.password );
        cmd.add( path );
        execute( cmd );
    }

    private void addKeychain( String[] keychains, String path ) {

        List<String> cmd = new LinkedList<String>();
        cmd.add( "security" );
        cmd.add( "list-keychains" );
        cmd.add( "-s" );
        for (String keychain : keychains) {
            cmd.add( keychain );
        }
        cmd.add( path );
        execute( cmd );
    }

    private void unlockKeychain( Keychain keychain, String keychainPath ) {

        List<String> cmd = new LinkedList<String>();
        cmd.add( "security" );
        cmd.add( "unlock-keychain" );
        cmd.add( "-p" );
        cmd.add( keychain.password );
        cmd.add( keychainPath );
        execute( cmd );
    }
}
